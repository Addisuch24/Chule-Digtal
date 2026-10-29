const pool = require("../config/db");

async function findFamilyMemberByUsername(username) {
  const normalized = username ? username.trim().toLowerCase() : "";
  const result = await pool.query(
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
      WHERE LOWER(username) = LOWER($1)
      LIMIT 1
    `,
    [normalized]
  );

  return result.rows[0] || null;
}

async function findFamilyMemberByEmail(email) {
  if (!email) return null;
  const normalized = email.trim().toLowerCase();
  const result = await pool.query(
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
      WHERE LOWER(email) = LOWER($1)
      LIMIT 1
    `,
    [normalized]
  );

  return result.rows[0] || null;
}

async function findFamilyMemberById(id) {
  const result = await pool.query(
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
      WHERE id = $1
      LIMIT 1
    `,
    [id]
  );

  return result.rows[0] || null;
}

async function createFamilyMember(
  fullName,
  phone,
  email,
  username,
  passwordHash,
  isActive = 0
) {
  const result = await pool.query(
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
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING id
    `,
    [
      fullName.trim(),
      phone && phone.trim() ? phone.trim() : null,
      email && email.trim() ? email.trim().toLowerCase() : null,
      username.trim().toLowerCase(),
      passwordHash,
      isActive ? 1 : 0,
    ]
  );

  return result.rows[0].id;
}

module.exports = {
  findFamilyMemberByUsername,
  findFamilyMemberByEmail,
  findFamilyMemberById,
  createFamilyMember,
};
