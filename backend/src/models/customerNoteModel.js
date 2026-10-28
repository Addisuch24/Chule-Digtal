const pool = require("../config/db");

async function createCustomerNote({
  customerId,
  noteText,
  createdBy,
}) {
  const insertResult = await pool.query(
    `
      INSERT INTO customer_notes (
        customer_id,
        note_text,
        created_by
      )
      VALUES ($1, $2, $3)
      RETURNING id
    `,
    [customerId, noteText, createdBy]
  );

  const newId = insertResult.rows[0].id;

  const result = await pool.query(
    `
      SELECT
        n.id,
        n.customer_id,
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
      WHERE n.id = $1
    `,
    [newId]
  );

  return result.rows[0];
}

async function getCustomerNotes(customerId) {
  const result = await pool.query(
    `
      SELECT
        n.id,
        n.customer_id,
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
      WHERE n.customer_id = $1
      ORDER BY n.created_at DESC
    `,
    [customerId]
  );

  return result.rows;
}

async function deleteCustomerNote(noteId) {
  const result = await pool.query(
    "DELETE FROM customer_notes WHERE id = $1",
    [noteId]
  );
  return result.rowCount > 0;
}

async function deleteCustomerNotes(noteIds) {
  if (!Array.isArray(noteIds) || noteIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const validIds = noteIds.map(Number).filter((id) => Number.isInteger(id) && id > 0);
  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const result = await pool.query(
    "DELETE FROM customer_notes WHERE id = ANY($1::bigint[])",
    [validIds]
  );

  return {
    deletedCount: result.rowCount,
    deletedIds: validIds,
  };
}

module.exports = {
  createCustomerNote,
  getCustomerNotes,
  deleteCustomerNote,
  deleteCustomerNotes,
};