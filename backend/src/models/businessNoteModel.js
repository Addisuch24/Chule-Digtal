const pool = require("../config/db");

async function createBusinessNote({ noteText, createdBy }) {
  const insertResult = await pool.query(
    `
      INSERT INTO business_notes (
        note_text,
        created_by
      )
      VALUES ($1, $2)
      RETURNING id
    `,
    [noteText, createdBy]
  );

  const newId = insertResult.rows[0].id;

  const result = await pool.query(
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
      WHERE bn.id = $1
    `,
    [newId]
  );

  return result.rows[0];
}

async function getAllBusinessNotes() {
  const result = await pool.query(
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

  return result.rows;
}

async function getBusinessNoteById(noteId) {
  const result = await pool.query(
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
      WHERE bn.id = $1
    `,
    [noteId]
  );

  return result.rows[0] || null;
}

async function updateBusinessNote({ noteId, noteText }) {
  await pool.query(
    `
      UPDATE business_notes
      SET note_text = $1,
          updated_at = CURRENT_TIMESTAMP
      WHERE id = $2
    `,
    [noteText, noteId]
  );

  return getBusinessNoteById(noteId);
}

async function deleteBusinessNote(noteId) {
  const result = await pool.query(
    "DELETE FROM business_notes WHERE id = $1",
    [noteId]
  );

  return result.rowCount > 0;
}

async function deleteBusinessNotes(noteIds) {
  if (!Array.isArray(noteIds) || noteIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const validIds = noteIds.map(Number).filter((id) => Number.isInteger(id) && id > 0);
  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }

  const result = await pool.query(
    "DELETE FROM business_notes WHERE id = ANY($1::bigint[])",
    [validIds]
  );

  return {
    deletedCount: result.rowCount,
    deletedIds: validIds,
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
