const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

const authModel = require("../models/authModel");

async function login(username, password) {
  const normalizedUsername = username ? username.trim().toLowerCase() : "";
  const user = await authModel.findFamilyMemberByUsername(normalizedUsername);

  if (!user) {
    console.warn(`[AUTH DIAGNOSTIC] Login failed: User '${normalizedUsername}' not found in database.`);
    return null;
  }

  const isActiveBool = Boolean(user.is_active);
  if (!isActiveBool) {
    console.warn(`[AUTH DIAGNOSTIC] Login failed: User '${normalizedUsername}' (ID: ${user.id}) is inactive (is_active = 0).`);
    return null;
  }

  const passwordMatches = await bcrypt.compare(
    password,
    user.password_hash
  );

  if (!passwordMatches) {
    console.warn(`[AUTH DIAGNOSTIC] Login failed: Password mismatch for user '${normalizedUsername}' (ID: ${user.id}).`);
    return null;
  }

  console.log(`[AUTH DIAGNOSTIC] Login successful for user '${normalizedUsername}' (ID: ${user.id}).`);

  const secret = process.env.JWT_SECRET || "chule_digita_change_this_to_a_long_random_secret";
  const token = jwt.sign(
    {
      id: user.id,
      username: user.username,
    },
    secret,
    {
      expiresIn: process.env.JWT_EXPIRES_IN || "7d",
    }
  );

  return {
    token,
    user: {
      id: user.id,
      full_name: user.full_name,
      phone: user.phone,
      email: user.email,
      username: user.username,
      is_active: user.is_active ? 1 : 0,
      created_at: user.created_at,
    },
  };
}

async function register(
  fullName,
  phone,
  email,
  username,
  password
) {
  const normalizedUsername = username.trim().toLowerCase();
  const normalizedEmail = email ? email.trim().toLowerCase() : "";

  const existingUsername = await authModel.findFamilyMemberByUsername(normalizedUsername);
  if (existingUsername) {
    const error = new Error("Username already exists");
    error.code = "USERNAME_EXISTS";
    throw error;
  }

  if (normalizedEmail) {
    const existingEmail = await authModel.findFamilyMemberByEmail(normalizedEmail);
    if (existingEmail) {
      const error = new Error("Google account email is already registered");
      error.code = "EMAIL_EXISTS";
      throw error;
    }
  }

  const passwordHash = await bcrypt.hash(password, 10);

  const userId = await authModel.createFamilyMember(
    fullName.trim(),
    phone && phone.trim() ? phone.trim() : null,
    normalizedEmail || null,
    normalizedUsername,
    passwordHash
  );

  const createdUser = await authModel.findFamilyMemberById(userId);
  if (createdUser) {
    createdUser.is_active = createdUser.is_active ? 1 : 0;
  }
  return createdUser;
}

async function getProfile(userId) {
  const user = await authModel.findFamilyMemberById(userId);
  if (!user || !user.is_active) {
    return null;
  }
  return {
    id: user.id,
    full_name: user.full_name,
    phone: user.phone,
    email: user.email,
    username: user.username,
    is_active: user.is_active ? 1 : 0,
    created_at: user.created_at,
    updated_at: user.updated_at,
  };
}

module.exports = {
  login,
  register,
  getProfile,
};
