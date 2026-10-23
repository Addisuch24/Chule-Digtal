const pool = require("../config/db");

async function createBusinessNote({ noteText, createdBy }) {
  const [result] = await pool.execute(
    `
      INSERT INTO business_notes (
        note_text,
        created_by
      )
      VALUES (?, ?)
    `,
    [noteText, createdBy]
  );

  const [rows] = await pool.execute(
    `
      SELECT
        bn.id,
        bn.note_text,
        bn.created_by,
        fm.full_name AS created_by_name,
        bn.created_at,
        bn.updated_at
      FROM business_notes bn
      JOIN family_members fm
        ON fm.id = bn.created_by
      WHERE bn.id = ?
    `,
    [result.insertId]
  );

  return rows[0];
}

async function getAllBusinessNotes() {
  const [rows] = await pool.execute(
    `
      SELECT
        bn.id,
        bn.note_text,
        bn.created_by,
        fm.full_name AS created_by_name,
        bn.created_at,
        bn.updated_at
      FROM business_notes bn
      JOIN family_members fm
        ON fm.id = bn.created_by
      ORDER BY bn.created_at DESC
    `
  );

  return rows;
}

async function getBusinessNoteById(noteId) {
  const [rows] = await pool.execute(
    `
      SELECT
        bn.id,
        bn.note_text,
        bn.created_by,
        fm.full_name AS created_by_name,
        bn.created_at,
        bn.updated_at
      FROM business_notes bn
      JOIN family_members fm
        ON fm.id = bn.created_by
      WHERE bn.id = ?
    `,
    [noteId]
  );

  return rows[0] || null;
}

async function updateBusinessNote({ noteId, noteText }) {
  const [result] = await pool.execute(
    `
      UPDATE business_notes
      SET note_text = ?
      WHERE id = ?
    `,
    [noteText, noteId]
  );

  if (result.affectedRows === 0) {
    return getBusinessNoteById(noteId);
  }

  return getBusinessNoteById(noteId);
}

async function deleteBusinessNote(noteId) {
  const [result] = await pool.execute(
    "DELETE FROM business_notes WHERE id = ?",
    [noteId]
  );

  return result.affectedRows > 0;
}

async function deleteBusinessNotes(noteIds) {
  if (!Array.isArray(noteIds) || noteIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const [result] = await pool.query(
    "DELETE FROM business_notes WHERE id IN (?)",
    [noteIds]
  );

  return {
    deletedCount: result.affectedRows,
    deletedIds: noteIds,
  };
}

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
  getBusinessNoteById,
  updateBusinessNote,
  deleteBusinessNote,
  deleteBusinessNotes,
};
