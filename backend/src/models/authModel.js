const pool = require("../config/db");

async function findFamilyMemberByUsername(username) {
  const [rows] = await pool.execute(
    `
      SELECT
        id,
        full_name,
        phone,
        username,
        password_hash,
        is_active
      FROM family_members
      WHERE username = ?
      LIMIT 1
    `,
    [username]
  );

  return rows[0] || null;
}

async function findFamilyMemberById(id) {
  const [rows] = await pool.execute(
    `
      SELECT
        id,
        full_name,
        phone,
        username,
        is_active
      FROM family_members
      WHERE id = ?
      LIMIT 1
    `,
    [id]
  );

  return rows[0] || null;
}

module.exports = {
  findFamilyMemberByUsername,
  findFamilyMemberById,
};