const pool = require("../config/db");

async function familyMemberExists(familyMemberId) {
  const result = await pool.query(
    `
      SELECT id
      FROM family_members
      WHERE id = $1
        AND is_active = 1
    `,
    [familyMemberId]
  );

  return result.rows.length > 0;
}

async function findExistingCustomer({ fullName, phone }) {
  const normalizedPhone = phone ? String(phone).replace(/\D/g, "") : null;
  const trimmedName = fullName ? fullName.trim().replace(/\s+/g, " ") : "";

  // 1. Try matching by phone if normalized digits exist (and at least 7 digits)
  if (normalizedPhone && normalizedPhone.length >= 7) {
    const phoneSuffix = normalizedPhone.slice(-9);
    const result = await pool.query(
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
        WHERE REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(c.phone, ''), ' ', ''), '-', ''), '+', ''), '(', '') LIKE CONCAT('%', $1::text)
           OR $2::text LIKE CONCAT('%', REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(c.phone, ''), ' ', ''), '-', ''), '+', ''), '(', ''))
        LIMIT 1
      `,
      [phoneSuffix, phoneSuffix]
    );
    if (result.rows.length > 0) return result.rows[0];
  }

  // 2. Try matching by case-insensitive name
  if (trimmedName) {
    const result = await pool.query(
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
        WHERE LOWER(TRIM(c.full_name)) = LOWER($1)
        LIMIT 1
      `,
      [trimmedName]
    );
    if (result.rows.length > 0) return result.rows[0];
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
      await pool.query(
        `UPDATE customers SET full_name = $1, phone = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3`,
        [cleanName, updatedPhone, existing.id]
      );
    }
    return getCustomerById(existing.id);
  }

  const cleanName = fullName.trim().replace(/\s+/g, " ");
  const result = await pool.query(
    `
      INSERT INTO customers (
        full_name,
        phone,
        created_by
      )
      VALUES ($1, $2, $3)
      RETURNING id
    `,
    [cleanName, phone || null, createdBy]
  );

  const newId = result.rows[0].id;
  return getCustomerById(newId);
}

async function updateCustomer({ customerId, fullName, phone }) {
  const customer = await getCustomerById(customerId);
  if (!customer) return null;

  const cleanName = fullName && fullName.trim() ? fullName.trim().replace(/\s+/g, " ") : customer.full_name;
  const updatedPhone = phone !== undefined ? (phone && phone.trim() ? phone.trim() : null) : customer.phone;

  await pool.query(
    `UPDATE customers SET full_name = $1, phone = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3`,
    [cleanName, updatedPhone, customerId]
  );

  return getCustomerById(customerId);
}

async function getCustomerById(customerId) {
  const customerRes = await pool.query(
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
      WHERE c.id = $1
    `,
    [customerId]
  );

  if (customerRes.rows.length === 0) {
    return null;
  }

  const customer = customerRes.rows[0];

  const noteRes = await pool.query(
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
      WHERE n.customer_id = $1
      ORDER BY n.created_at DESC
    `,
    [customerId]
  );

  customer.notes = noteRes.rows;

  return customer;
}

async function getAllCustomers() {
  const result = await pool.query(
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
        ) AS "totalDebit",

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
        ) AS "totalCredit"

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

  return result.rows.map((customer) => {
    const totalDebit = Number(customer.totalDebit ?? customer.totaldebit ?? 0);
    const totalCredit = Number(customer.totalCredit ?? customer.totalcredit ?? 0);

    return {
      ...customer,
      totalDebit,
      totalCredit,
      balance: totalCredit - totalDebit,
    };
  });
}

async function getCustomerFinancialMovementCount(customerId) {
  const result = await pool.query(
    "SELECT COUNT(*) AS count FROM financial_movements WHERE customer_id = $1",
    [customerId]
  );
  return Number(result.rows[0]?.count || 0);
}

async function deleteCustomer(customerId) {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");

    const custRes = await client.query(
      "SELECT id, full_name FROM customers WHERE id = $1",
      [customerId]
    );

    if (custRes.rows.length === 0) {
      await client.query("ROLLBACK");
      return { success: false, blocked: false, reason: "Customer not found" };
    }

    const targetName = custRes.rows[0].full_name.trim();
    const matchingRes = await client.query(
      "SELECT id FROM customers WHERE LOWER(TRIM(full_name)) = LOWER($1)",
      [targetName]
    );
    const targetIds = matchingRes.rows.map((r) => r.id);

    // 1. Delete associated financial movements
    await client.query(
      "DELETE FROM financial_movements WHERE customer_id = ANY($1::bigint[])",
      [targetIds]
    );

    // 2. Delete associated customer notes
    await client.query(
      "DELETE FROM customer_notes WHERE customer_id = ANY($1::bigint[])",
      [targetIds]
    );

    // 3. Detach orders so order history is preserved
    await client.query(
      "UPDATE orders SET customer_id = NULL WHERE customer_id = ANY($1::bigint[])",
      [targetIds]
    );

    // 4. Delete customer row(s)
    const deleteRes = await client.query(
      "DELETE FROM customers WHERE id = ANY($1::bigint[])",
      [targetIds]
    );

    await client.query("COMMIT");
    return {
      success: deleteRes.rowCount > 0,
      blocked: false,
      deletedCount: deleteRes.rowCount,
      deletedName: targetName,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
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

  const client = await pool.connect();
  try {
    await client.query("BEGIN");

    const matchingRes = await client.query(
      `SELECT id FROM customers WHERE LOWER(TRIM(full_name)) IN (
        SELECT LOWER(TRIM(full_name)) FROM customers WHERE id = ANY($1::bigint[])
      ) OR id = ANY($1::bigint[])`,
      [validIds]
    );
    const allTargetIds = Array.from(new Set(matchingRes.rows.map((r) => Number(r.id))));

    // 1. Delete associated financial movements
    await client.query(
      "DELETE FROM financial_movements WHERE customer_id = ANY($1::bigint[])",
      [allTargetIds]
    );

    // 2. Delete associated customer notes
    await client.query(
      "DELETE FROM customer_notes WHERE customer_id = ANY($1::bigint[])",
      [allTargetIds]
    );

    // 3. Detach orders
    await client.query(
      "UPDATE orders SET customer_id = NULL WHERE customer_id = ANY($1::bigint[])",
      [allTargetIds]
    );

    // 4. Delete customer rows
    const deleteRes = await client.query(
      "DELETE FROM customers WHERE id = ANY($1::bigint[])",
      [allTargetIds]
    );

    await client.query("COMMIT");
    return {
      deletedCount: deleteRes.rowCount,
      deletedIds: allTargetIds,
      skipped: [],
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
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