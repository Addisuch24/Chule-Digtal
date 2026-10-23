const pool = require("../config/db");
async function familyMemberExists(familyMemberId) {
  const [rows] = await pool.execute(
    `
      SELECT id
      FROM family_members
      WHERE id = ?
        AND is_active = TRUE
    `,
    [familyMemberId]
  );

  return rows.length > 0;
}
async function findExistingCustomer({ fullName, phone }) {
  const normalizedPhone = phone ? String(phone).replace(/\D/g, "") : null;
  const trimmedName = fullName ? fullName.trim().replace(/\s+/g, " ") : "";

  // 1. Try matching by phone if normalized digits exist (and at least 7 digits)
  if (normalizedPhone && normalizedPhone.length >= 7) {
    const [rows] = await pool.execute(
      `
        SELECT
          c.id,
          c.full_name,
          c.phone,
          c.created_by,
          fm.full_name AS created_by_name,
          c.created_at,
          c.updated_at
        FROM customers c
        LEFT JOIN family_members fm
          ON fm.id = c.created_by
        WHERE REPLACE(REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '-', ''), '+', ''), '(', '') LIKE CONCAT('%', ?)
           OR ? LIKE CONCAT('%', REPLACE(REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '-', ''), '+', ''), '(', ''))
        LIMIT 1
      `,
      [normalizedPhone.slice(-9), normalizedPhone.slice(-9)]
    );
    if (rows.length > 0) return rows[0];
  }

  // 2. Try matching by case-insensitive name
  if (trimmedName) {
    const [rows] = await pool.execute(
      `
        SELECT
          c.id,
          c.full_name,
          c.phone,
          c.created_by,
          fm.full_name AS created_by_name,
          c.created_at,
          c.updated_at
        FROM customers c
        LEFT JOIN family_members fm
          ON fm.id = c.created_by
        WHERE LOWER(TRIM(c.full_name)) = LOWER(?)
        LIMIT 1
      `,
      [trimmedName]
    );
    if (rows.length > 0) return rows[0];
  }

  return null;
}

async function createCustomer({ fullName, phone, createdBy }) {
  const existing = await findExistingCustomer({ fullName, phone });
  if (existing) {
    const updatedPhone = phone !== undefined && phone !== null && phone.trim() !== "" 
      ? phone.trim() 
      : existing.phone;
    const cleanName = fullName && fullName.trim() ? fullName.trim().replace(/\s+/g, " ") : existing.full_name;

    if (updatedPhone !== existing.phone || cleanName !== existing.full_name) {
      await pool.execute(
        `UPDATE customers SET full_name = ?, phone = ? WHERE id = ?`,
        [cleanName, updatedPhone, existing.id]
      );
    }
    return getCustomerById(existing.id);
  }

  const cleanName = fullName.trim().replace(/\s+/g, " ");
  const [result] = await pool.execute(
    `
      INSERT INTO customers (
        full_name,
        phone,
        created_by
      )
      VALUES (?, ?, ?)
    `,
    [cleanName, phone || null, createdBy]
  );

  const [rows] = await pool.execute(
    `
      SELECT
        c.id,
        c.full_name,
        c.phone,
        c.created_by,
        fm.full_name AS created_by_name,
        c.created_at,
        c.updated_at
      FROM customers c
      LEFT JOIN family_members fm
        ON fm.id = c.created_by
      WHERE c.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
}

async function updateCustomer({ customerId, fullName, phone }) {
  const customer = await getCustomerById(customerId);
  if (!customer) return null;

  const cleanName = fullName && fullName.trim() ? fullName.trim().replace(/\s+/g, " ") : customer.full_name;
  const updatedPhone = phone !== undefined ? (phone && phone.trim() ? phone.trim() : null) : customer.phone;

  await pool.execute(
    `UPDATE customers SET full_name = ?, phone = ? WHERE id = ?`,
    [cleanName, updatedPhone, customerId]
  );

  return getCustomerById(customerId);
}
 async function getCustomerById(customerId) {
  const [customerRows] = await pool.execute(
    `
      SELECT
        c.id,
        c.full_name,
        c.phone,
        c.created_by,
        fm.full_name AS created_by_name,
        c.created_at,
        c.updated_at
      FROM customers c
      LEFT JOIN family_members fm
        ON fm.id = c.created_by
      WHERE c.id = ?
    `,
    [customerId]
  );

  if (customerRows.length === 0) {
    return null;
  }

  const customer = customerRows[0];

  const [noteRows] = await pool.execute(
    `
      SELECT
        n.id,
        n.note_text,
        n.created_by,
        fm.full_name AS created_by_name,
        n.verification_status,
        n.verified_by,
        n.verified_at,
        n.verification_note,
        n.created_at,
        n.updated_at
      FROM customer_notes n
      LEFT JOIN family_members fm
        ON fm.id = n.created_by
      WHERE n.customer_id = ?
      ORDER BY n.created_at DESC
    `,
    [customerId]
  );

  customer.notes = noteRows;

  return customer;
}
 async function getAllCustomers() {
  const [rows] = await pool.execute(
    `
      SELECT
        MIN(c.id) AS id,
        MIN(c.full_name) AS full_name,
        COALESCE(MAX(c.phone), MIN(c.phone)) AS phone,
        MIN(c.created_by) AS created_by,
        MIN(fm.full_name) AS created_by_name,
        MIN(c.created_at) AS created_at,
        MAX(c.updated_at) AS updated_at,

        COALESCE(
          SUM(
            CASE
              WHEN f.movement_type = 'DEBIT'
                   AND f.verification_status <> 'REJECTED'
              THEN f.amount
              ELSE 0
            END
          ),
          0
        ) AS totalDebit,

        COALESCE(
          SUM(
            CASE
              WHEN f.movement_type = 'CREDIT'
                   AND f.verification_status <> 'REJECTED'
              THEN f.amount
              ELSE 0
            END
          ),
          0
        ) AS totalCredit

      FROM customers c

      LEFT JOIN family_members fm
        ON fm.id = c.created_by

      LEFT JOIN financial_movements f
        ON f.customer_id = c.id

      GROUP BY
        LOWER(TRIM(c.full_name))

      ORDER BY id DESC
    `
  );

  return rows.map((customer) => {
    const totalDebit = Number(customer.totalDebit);
    const totalCredit = Number(customer.totalCredit);

    return {
      ...customer,
      totalDebit,
      totalCredit,
      balance: totalCredit - totalDebit,
    };
  });
}
async function getCustomerFinancialMovementCount(customerId) {
  const [rows] = await pool.execute(
    "SELECT COUNT(*) AS count FROM financial_movements WHERE customer_id = ?",
    [customerId]
  );
  return Number(rows[0].count);
}

async function deleteCustomer(customerId) {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const [custRows] = await connection.execute(
      "SELECT id, full_name FROM customers WHERE id = ?",
      [customerId]
    );

    if (custRows.length === 0) {
      await connection.rollback();
      return { success: false, blocked: false, reason: "Customer not found" };
    }

    const targetName = custRows[0].full_name.trim();
    const [matchingRows] = await connection.execute(
      "SELECT id FROM customers WHERE LOWER(TRIM(full_name)) = LOWER(?)",
      [targetName]
    );
    const targetIds = matchingRows.map((r) => r.id);

    // 1. Delete associated financial movements
    await connection.query(
      "DELETE FROM financial_movements WHERE customer_id IN (?)",
      [targetIds]
    );

    // 2. Delete associated customer notes
    await connection.query(
      "DELETE FROM customer_notes WHERE customer_id IN (?)",
      [targetIds]
    );

    // 3. Detach orders so order history is preserved
    await connection.query(
      "UPDATE orders SET customer_id = NULL WHERE customer_id IN (?)",
      [targetIds]
    );

    // 4. Delete customer row(s)
    const [result] = await connection.query(
      "DELETE FROM customers WHERE id IN (?)",
      [targetIds]
    );

    await connection.commit();
    return {
      success: result.affectedRows > 0,
      blocked: false,
      deletedCount: result.affectedRows,
      deletedName: targetName,
    };
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

async function deleteCustomers(customerIds) {
  if (!Array.isArray(customerIds) || customerIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  const validIds = customerIds
    .map((id) => Number(id))
    .filter((id) => Number.isInteger(id) && id > 0);

  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const [matchingRows] = await connection.query(
      `SELECT id FROM customers WHERE LOWER(TRIM(full_name)) IN (
        SELECT LOWER(TRIM(full_name)) FROM customers WHERE id IN (?)
      ) OR id IN (?)`,
      [validIds, validIds]
    );
    const allTargetIds = Array.from(new Set(matchingRows.map((r) => Number(r.id))));

    // 1. Delete associated financial movements
    await connection.query(
      "DELETE FROM financial_movements WHERE customer_id IN (?)",
      [allTargetIds]
    );

    // 2. Delete associated customer notes
    await connection.query(
      "DELETE FROM customer_notes WHERE customer_id IN (?)",
      [allTargetIds]
    );

    // 3. Detach orders
    await connection.query(
      "UPDATE orders SET customer_id = NULL WHERE customer_id IN (?)",
      [allTargetIds]
    );

    // 4. Delete customer rows
    const [result] = await connection.query(
      "DELETE FROM customers WHERE id IN (?)",
      [allTargetIds]
    );

    await connection.commit();
    return {
      deletedCount: result.affectedRows,
      deletedIds: allTargetIds,
      skipped: [],
    };
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

module.exports = {
  familyMemberExists,
  createCustomer,
  updateCustomer,
  findExistingCustomer,
  getCustomerById,
  getAllCustomers,
  getCustomerFinancialMovementCount,
  deleteCustomer,
  deleteCustomers,
};