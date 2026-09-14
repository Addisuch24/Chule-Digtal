const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

const authModel = require("../models/authModel");

async function login(username, password) {
  const user =
    await authModel.findFamilyMemberByUsername(
      username
    );

  if (!user) {
    return null;
  }

  if (!user.is_active) {
    return null;
  }

  const passwordMatches = await bcrypt.compare(
    password,
    user.password_hash
  );

  if (!passwordMatches) {
    return null;
  }

  const token = jwt.sign(
    {
      id: user.id,
      username: user.username,
    },
    process.env.JWT_SECRET,
    {
      expiresIn:
        process.env.JWT_EXPIRES_IN || "7d",
    }
  );

  return {
    token,

    user: {
      id: user.id,
      full_name: user.full_name,
      phone: user.phone,
      username: user.username,
      is_active: user.is_active,
    },
  };
}

async function register(
  fullName,
  phone,
  username,
  password
) {
  const existingUser =
    await authModel.findFamilyMemberByUsername(
      username
    );

  if (existingUser) {
    const error = new Error(
      "Username already exists"
    );

    error.code = "USERNAME_EXISTS";

    throw error;
  }

  const passwordHash = await bcrypt.hash(
    password,
    10
  );

  const userId =
    await authModel.createFamilyMember(
      fullName,
      phone,
      username,
      passwordHash
    );

  const createdUser =
    await authModel.findFamilyMemberById(
      userId
    );

  return createdUser;
}

module.exports = {
  login,
  register,
};