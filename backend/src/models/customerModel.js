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
async function createCustomer({ fullName, phone, createdBy }) {
  const [result] = await pool.execute(
    `
      INSERT INTO customers (
        full_name,
        phone,
        created_by
      )
      VALUES (?, ?, ?)
    `,
    [fullName, phone || null, createdBy]
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
      JOIN family_members fm
        ON fm.id = c.created_by
      WHERE c.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
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
      JOIN family_members fm
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
      JOIN family_members fm
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
        c.id,
        c.full_name,
        c.phone,
        c.created_by,
        fm.full_name AS created_by_name,
        c.created_at,
        c.updated_at,

        COALESCE(
          SUM(
            CASE
              WHEN f.movement_type = 'DEBIT'
                   AND f.verification_status = 'VERIFIED'
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
                   AND f.verification_status = 'VERIFIED'
              THEN f.amount
              ELSE 0
            END
          ),
          0
        ) AS totalCredit

      FROM customers c

      JOIN family_members fm
        ON fm.id = c.created_by

      LEFT JOIN financial_movements f
        ON f.customer_id = c.id

      GROUP BY
        c.id,
        c.full_name,
        c.phone,
        c.created_by,
        fm.full_name,
        c.created_at,
        c.updated_at

      ORDER BY c.id DESC
    `
  );

  return rows.map((customer) => {
    const totalDebit = Number(customer.totalDebit);
    const totalCredit = Number(customer.totalCredit);

    return {
      ...customer,
      totalDebit,
      totalCredit,
      balance: totalDebit - totalCredit,
    };
  });
}
module.exports = {
  familyMemberExists,
  createCustomer,
  getCustomerById,
  getAllCustomers,
};