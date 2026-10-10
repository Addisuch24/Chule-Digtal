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

module.exports = {
  createBusinessNote,
  getAllBusinessNotes,
  getBusinessNoteById,
};
