const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

const authModel = require("../models/authModel");

async function login(username, password) {
  const member =
    await authModel.findFamilyMemberByUsername(username);

  if (!member) {
    return null;
  }

  if (!member.is_active) {
    return null;
  }

  const passwordMatches = await bcrypt.compare(
    password,
    member.password_hash
  );

  if (!passwordMatches) {
    return null;
  }

  const token = jwt.sign(
    {
      userId: member.id,
      username: member.username,
    },
    process.env.JWT_SECRET,
    {
      expiresIn: process.env.JWT_EXPIRES_IN || "7d",
    }
  );

  return {
    token,
    user: {
      id: member.id,
      fullName: member.full_name,
      phone: member.phone,
      username: member.username,
    },
  };
}

async function getUserById(userId) {
  return authModel.findFamilyMemberById(userId);
}

module.exports = {
  login,
  getUserById,
};