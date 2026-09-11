const pool = require("../config/db");

async function createFinancialMovement({
  customerId,
  amount,
  movementType,
  description,
    idempotencyKey, 
  referenceType,
  referenceId,
  recordedBy,
  transactionAt,
}) {
  if (idempotencyKey) {
  const [existingRows] = await pool.execute(
    `
      SELECT *
      FROM financial_movements
      WHERE idempotency_key = ?
      LIMIT 1
    `,
    [idempotencyKey]
  );

  if (existingRows.length > 0) {
    return {
      ...existingRows[0],
      amount: Number(existingRows[0].amount),
      duplicate: true,
    };
  }
}
  const [result] = await pool.execute(
    `
      INSERT INTO financial_movements (
        customer_id,
        amount,
        movement_type,
        description,
        reference_type,
        reference_id,
        recorded_by,
        transaction_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `,
    [
      customerId,
      amount,
      movementType,
      description || null,
      referenceType || null,
      referenceId || null,
      recordedBy,
      transactionAt || new Date(),
    ]
  );

  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.recorded_by,
        recorder.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f
      JOIN customers c
        ON c.id = f.customer_id
      JOIN family_members recorder
        ON recorder.id = f.recorded_by
      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by
      WHERE f.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
}

async function getFinancialMovementById(movementId) {
  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.recorded_by,
        recorder.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f
      JOIN customers c
        ON c.id = f.customer_id
      JOIN family_members recorder
        ON recorder.id = f.recorded_by
      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by
      WHERE f.id = ?
    `,
    [movementId]
  );

  if (rows.length === 0) {
    return null;
  }

  return rows[0];
}

async function getAllFinancialMovements() {
  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.recorded_by,
        recorder.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f
      JOIN customers c
        ON c.id = f.customer_id
      JOIN family_members recorder
        ON recorder.id = f.recorded_by
      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by
      ORDER BY f.transaction_at DESC, f.id DESC
    `
  );

  return rows;
}
async function verifyFinancialMovement({
  movementId,
  verifiedBy,
  verificationNote,
}) {
  const [result] = await pool.execute(
    `
      UPDATE financial_movements
      SET
        verification_status = 'VERIFIED',
        verified_by = ?,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = ?
      WHERE id = ?
        AND verification_status = 'PENDING'
    `,
    [
      verifiedBy,
      verificationNote || null,
      movementId,
    ]
  );

  if (result.affectedRows === 0) {
    return null;
  }

  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.recorded_by,
        recorder.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f
      JOIN customers c
        ON c.id = f.customer_id
      JOIN family_members recorder
        ON recorder.id = f.recorded_by
      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by
      WHERE f.id = ?
    `,
    [movementId]
  );

  return rows[0];
}
async function rejectFinancialMovement({
  movementId,
  verifiedBy,
  verificationNote,
}) {
  const [result] = await pool.execute(
    `
      UPDATE financial_movements
      SET
        verification_status = 'REJECTED',
        verified_by = ?,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = ?
      WHERE id = ?
        AND verification_status = 'PENDING'
    `,
    [
      verifiedBy,
      verificationNote,
      movementId,
    ]
  );

  if (result.affectedRows === 0) {
    return null;
  }

  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.recorded_by,
        recorder.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f
      JOIN customers c
        ON c.id = f.customer_id
      JOIN family_members recorder
        ON recorder.id = f.recorded_by
      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by
      WHERE f.id = ?
    `,
    [movementId]
  );

  return rows[0];
}
async function getCustomerBalance(customerId) {
  const [rows] = await pool.execute(
    `
      SELECT
        COALESCE(
          SUM(
            CASE
              WHEN movement_type = 'DEBIT'
                   AND verification_status = 'VERIFIED'
              THEN amount
              ELSE 0
            END
          ),
          0
        ) AS total_debit,

        COALESCE(
          SUM(
            CASE
              WHEN movement_type = 'CREDIT'
                   AND verification_status = 'VERIFIED'
              THEN amount
              ELSE 0
            END
          ),
          0
        ) AS total_credit
      FROM financial_movements
      WHERE customer_id = ?
    `,
    [customerId]
  );

  const totalDebit = Number(rows[0].total_debit);
  const totalCredit = Number(rows[0].total_credit);

  return {
    customerId,
    totalDebit,
    totalCredit,
    balance: totalDebit - totalCredit,
  };
}
async function getCustomerPayments(customerId) {
  const [rows] = await pool.execute(
    `
      SELECT
        f.id,
        f.customer_id,
        f.amount,
        f.movement_type,
        f.description,
        f.reference_type,
        f.reference_id,
        f.idempotency_key,
        f.recorded_by,
        fm.full_name AS recorded_by_name,
        f.verification_status,
        f.verified_by,
        verifier.full_name AS verified_by_name,
        f.verified_at,
        f.verification_note,
        f.transaction_at,
        f.created_at,
        f.updated_at
      FROM financial_movements f

      JOIN family_members fm
        ON fm.id = f.recorded_by

      LEFT JOIN family_members verifier
        ON verifier.id = f.verified_by

      WHERE f.customer_id = ?
        AND f.movement_type = 'CREDIT'

      ORDER BY f.transaction_at DESC, f.id DESC
    `,
    [customerId]
  );

  return rows.map((payment) => ({
    ...payment,
    amount: Number(payment.amount),
  }));
}
module.exports = {
  createFinancialMovement,
  getFinancialMovementById,
  getAllFinancialMovements,
  verifyFinancialMovement,
  rejectFinancialMovement,
  getCustomerBalance,
  getCustomerPayments,
  
};
