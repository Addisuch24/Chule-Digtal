const pool = require("../config/db");

async function createCustomerNote({
  customerId,
  noteText,
  createdBy,
}) {
  const [result] = await pool.execute(
    `
      INSERT INTO customer_notes (
        customer_id,
        note_text,
        created_by
      )
      VALUES (?, ?, ?)
    `,
    [customerId, noteText, createdBy]
  );

  const [rows] = await pool.execute(
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
      WHERE n.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
}
async function getCustomerNotes(customerId) {
  const [rows] = await pool.execute(
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
      WHERE n.customer_id = ?
      ORDER BY n.created_at DESC
    `,
    [customerId]
  );

  return rows;
}
async function deleteCustomerNote(noteId) {
  const [result] = await pool.execute(
    "DELETE FROM customer_notes WHERE id = ?",
    [noteId]
  );
  return result.affectedRows > 0;
}

async function deleteCustomerNotes(noteIds) {
  if (!Array.isArray(noteIds) || noteIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const [result] = await pool.query(
    "DELETE FROM customer_notes WHERE id IN (?)",
    [noteIds]
  );

  return {
    deletedCount: result.affectedRows,
    deletedIds: noteIds,
  };
}

module.exports = {
  createCustomerNote,
  getCustomerNotes,
  deleteCustomerNote,
  deleteCustomerNotes,
};