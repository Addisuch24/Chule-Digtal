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
    const existing = await pool.query(
      `
        SELECT *
        FROM financial_movements
        WHERE idempotency_key = $1
        LIMIT 1
      `,
      [idempotencyKey]
    );

    if (existing.rows.length > 0) {
      return {
        ...existing.rows[0],
        amount: Number(existing.rows[0].amount),
        duplicate: true,
      };
    }
  }

  const insertResult = await pool.query(
    `
      INSERT INTO financial_movements (
        customer_id,
        amount,
        movement_type,
        description,
        reference_type,
        reference_id,
        recorded_by,
        transaction_at,
        idempotency_key
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING id
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
      idempotencyKey || null,
    ]
  );

  const newId = insertResult.rows[0].id;

  const result = await pool.query(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        c.phone AS customer_phone,
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
      WHERE f.id = $1
    `,
    [newId]
  );

  return result.rows[0];
}

async function getFinancialMovementById(movementId) {
  const result = await pool.query(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        c.phone AS customer_phone,
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
      WHERE f.id = $1
    `,
    [movementId]
  );

  if (result.rows.length === 0) {
    return null;
  }

  return result.rows[0];
}

async function getAllFinancialMovements() {
  const result = await pool.query(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        c.phone AS customer_phone,
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

  return result.rows;
}

async function verifyFinancialMovement({
  movementId,
  verifiedBy,
  verificationNote,
}) {
  const updateResult = await pool.query(
    `
      UPDATE financial_movements
      SET
        verification_status = 'VERIFIED',
        verified_by = $1,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = $2,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $3
        AND verification_status = 'PENDING'
        AND recorded_by <> $1
    `,
    [
      verifiedBy,
      verificationNote || null,
      movementId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return null;
  }

  const result = await pool.query(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        c.phone AS customer_phone,
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
      WHERE f.id = $1
    `,
    [movementId]
  );

  return result.rows[0];
}

async function rejectFinancialMovement({
  movementId,
  verifiedBy,
  verificationNote,
}) {
  const updateResult = await pool.query(
    `
      UPDATE financial_movements
      SET
        verification_status = 'REJECTED',
        verified_by = $1,
        verified_at = CURRENT_TIMESTAMP,
        verification_note = $2,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $3
        AND verification_status = 'PENDING'
    `,
    [
      verifiedBy,
      verificationNote,
      movementId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return null;
  }

  const result = await pool.query(
    `
      SELECT
        f.id,
        f.customer_id,
        c.full_name AS customer_name,
        c.phone AS customer_phone,
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
      WHERE f.id = $1
    `,
    [movementId]
  );

  return result.rows[0];
}

async function updateFinancialMovement({
  movementId,
  customerId,
  amount,
  movementType,
  description,
}) {
  const rowRes = await pool.query(
    "SELECT id, verification_status FROM financial_movements WHERE id = $1",
    [movementId]
  );

  if (rowRes.rows.length === 0) {
    return { success: false, blocked: false, notFound: true };
  }

  if (rowRes.rows[0].verification_status === "VERIFIED") {
    return {
      success: false,
      blocked: true,
      reason: "Verified financial records cannot be modified to preserve accounting audit accuracy",
    };
  }

  const updateResult = await pool.query(
    `
      UPDATE financial_movements
      SET
        customer_id = $1,
        amount = $2,
        movement_type = $3,
        description = $4,
        verification_status = 'PENDING',
        verified_by = NULL,
        verified_at = NULL,
        verification_note = NULL,
        updated_at = CURRENT_TIMESTAMP
      WHERE id = $5
        AND verification_status <> 'VERIFIED'
    `,
    [
      customerId,
      amount,
      movementType,
      description !== undefined && description !== null ? description.trim() || null : null,
      movementId,
    ]
  );

  if (updateResult.rowCount === 0) {
    return null;
  }

  const updatedRecord = await getFinancialMovementById(movementId);
  return { success: true, blocked: false, data: updatedRecord };
}

async function deleteFinancialMovement(movementId) {
  const rowRes = await pool.query(
    "SELECT id, verification_status FROM financial_movements WHERE id = $1",
    [movementId]
  );

  if (rowRes.rows.length === 0) {
    return { success: false, blocked: false, notFound: true };
  }

  if (rowRes.rows[0].verification_status === "VERIFIED") {
    return {
      success: false,
      blocked: true,
      reason: "Verified financial records cannot be permanently deleted to preserve accounting audit accuracy",
    };
  }

  const result = await pool.query(
    "DELETE FROM financial_movements WHERE id = $1",
    [movementId]
  );

  return { success: result.rowCount > 0, blocked: false };
}

async function deleteFinancialMovements(movementIds) {
  if (!Array.isArray(movementIds) || movementIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  const validIds = movementIds.map(Number).filter((id) => Number.isInteger(id) && id > 0);
  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped: [] };
  }

  // Prevent deleting verified financial movements to preserve accounting audit accuracy
  const rowRes = await pool.query(
    "SELECT id, verification_status FROM financial_movements WHERE id = ANY($1::bigint[])",
    [validIds]
  );

  const eligibleIds = [];
  const skipped = [];

  for (const row of rowRes.rows) {
    if (row.verification_status === "VERIFIED") {
      skipped.push({
        id: Number(row.id),
        reason: "Verified financial records cannot be permanently deleted to preserve accounting audit accuracy",
      });
    } else {
      eligibleIds.push(Number(row.id));
    }
  }

  if (eligibleIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skipped };
  }

  const deleteRes = await pool.query(
    "DELETE FROM financial_movements WHERE id = ANY($1::bigint[])",
    [eligibleIds]
  );

  return {
    deletedCount: deleteRes.rowCount,
    deletedIds: eligibleIds,
    skipped,
  };
}

async function getCustomerBalance(customerId) {
  const result = await pool.query(
    `
      SELECT
        ROUND(
          COALESCE(
            SUM(
              CASE
                WHEN movement_type = 'DEBIT'
                     AND verification_status <> 'REJECTED'
                THEN amount
                ELSE 0
              END
            ),
            0
          ),
          2
        ) AS total_debit,

        ROUND(
          COALESCE(
            SUM(
              CASE
                WHEN movement_type = 'CREDIT'
                     AND verification_status <> 'REJECTED'
                THEN amount
                ELSE 0
              END
            ),
            0
          ),
          2
        ) AS total_credit,

        ROUND(
          COALESCE(
            SUM(
              CASE
                WHEN movement_type = 'CREDIT'
                     AND verification_status <> 'REJECTED'
                THEN amount
                ELSE 0
              END
            ),
            0
          ) -
          COALESCE(
            SUM(
              CASE
                WHEN movement_type = 'DEBIT'
                     AND verification_status <> 'REJECTED'
                THEN amount
                ELSE 0
              END
            ),
            0
          ),
          2
        ) AS balance
      FROM financial_movements
      WHERE customer_id = $1
    `,
    [customerId]
  );

  const totalDebit = Number(Number(result.rows[0]?.total_debit || 0).toFixed(2));
  const totalCredit = Number(Number(result.rows[0]?.total_credit || 0).toFixed(2));
  const balance = Number(Number(result.rows[0]?.balance || 0).toFixed(2));

  return {
    customerId,
    totalDebit,
    totalCredit,
    balance,
  };
}

async function getCustomerPayments(customerId) {
  const result = await pool.query(
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

      WHERE f.customer_id = $1
        AND f.movement_type = 'CREDIT'

      ORDER BY f.transaction_at DESC, f.id DESC
    `,
    [customerId]
  );

  return result.rows.map((payment) => ({
    ...payment,
    amount: Number(payment.amount),
  }));
}

module.exports = {
  createFinancialMovement,
  getFinancialMovementById,
  getAllFinancialMovements,
  updateFinancialMovement,
  deleteFinancialMovement,
  deleteFinancialMovements,
  verifyFinancialMovement,
  rejectFinancialMovement,
  getCustomerBalance,
  getCustomerPayments,
};
