const pool = require("../config/db");

async function findFamilyMemberByUsername(username) {
  const normalized = username ? username.trim().toLowerCase() : "";
  const [rows] = await pool.execute(
    `
      SELECT
        id,
        full_name,
        phone,
        email,
        username,
        password_hash,
        is_active,
        created_at,
        updated_at
      FROM family_members
      WHERE LOWER(username) = LOWER(?)
      LIMIT 1
    `,
    [normalized]
  );

  return rows[0] || null;
}

async function findFamilyMemberByEmail(email) {
  if (!email) return null;
  const normalized = email.trim().toLowerCase();
  const [rows] = await pool.execute(
    `
      SELECT
        id,
        full_name,
        phone,
        email,
        username,
        password_hash,
        is_active,
        created_at,
        updated_at
      FROM family_members
      WHERE LOWER(email) = LOWER(?)
      LIMIT 1
    `,
    [normalized]
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
        email,
        username,
        is_active,
        created_at,
        updated_at
      FROM family_members
      WHERE id = ?
      LIMIT 1
    `,
    [id]
  );

  return rows[0] || null;
}

async function createFamilyMember(
  fullName,
  phone,
  email,
  username,
  passwordHash
) {
  const [result] = await pool.execute(
    `
      INSERT INTO family_members
      (
        full_name,
        phone,
        email,
        username,
        password_hash,
        is_active
      )
      VALUES (?, ?, ?, ?, ?, 1)
    `,
    [
      fullName.trim(),
      phone && phone.trim() ? phone.trim() : null,
      email && email.trim() ? email.trim().toLowerCase() : null,
      username.trim().toLowerCase(),
      passwordHash,
    ]
  );

  return result.insertId;
}

module.exports = {
  findFamilyMemberByUsername,
  findFamilyMemberByEmail,
  findFamilyMemberById,
  createFamilyMember,
};
