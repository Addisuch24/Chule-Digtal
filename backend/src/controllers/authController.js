const authService = require("../services/authService");

async function login(req, res) {
  try {
    const { username, password } = req.body;

    if (!username || !username.trim()) {
      return res.status(400).json({
        success: false,
        message: "Username is required",
      });
    }

    if (!password) {
      return res.status(400).json({
        success: false,
        message: "Password is required",
      });
    }

    const result = await authService.login(
      username.trim(),
      password
    );

    if (!result) {
      return res.status(401).json({
        success: false,
        message: "Invalid username or password",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Login successful",
      data: result,
    });
  } catch (error) {
    console.error("[AUTH DIAGNOSTIC] Login exception:", error.message);

    return res.status(500).json({
      success: false,
      message: "Login failed",
    });
  }
}

async function register(req, res) {
  try {
    const {
      fullName,
      phone,
      email,
      googleAccount,
      username,
      password,
    } = req.body;

    const nameToUse = fullName || req.body.name;
    const emailToUse = email || googleAccount;

    if (!nameToUse || !nameToUse.trim()) {
      return res.status(400).json({
        success: false,
        message: "Full name is required",
      });
    }

    if (!emailToUse || !emailToUse.trim()) {
      return res.status(400).json({
        success: false,
        message: "Google account email is required",
      });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(emailToUse.trim())) {
      return res.status(400).json({
        success: false,
        message: "Please enter a valid Google account email address",
      });
    }

    if (!username || !username.trim()) {
      return res.status(400).json({
        success: false,
        message: "Username is required",
      });
    }

    if (!password) {
      return res.status(400).json({
        success: false,
        message: "Password is required",
      });
    }

    if (password.length < 4) {
      return res.status(400).json({
        success: false,
        message: "Password must be at least 4 characters",
      });
    }

    const result = await authService.register(
      nameToUse.trim(),
      phone ? phone.trim() : null,
      emailToUse.trim(),
      username.trim(),
      password
    );

    return res.status(201).json({
      success: true,
      message: "Family member registered successfully",
      data: result,
    });
  } catch (error) {
    console.error("Registration error:", error);

    if (error.code === "USERNAME_EXISTS") {
      return res.status(409).json({
        success: false,
        message: "Username already exists",
      });
    }

    if (error.code === "EMAIL_EXISTS") {
      return res.status(409).json({
        success: false,
        message: "Google account email is already registered",
      });
    }

    if (error.code === "ER_DUP_ENTRY" || error.code === "23505") {
      const isEmail =
        (error.message && error.message.includes("email")) ||
        (error.detail && error.detail.includes("email"));
      if (isEmail) {
        return res.status(409).json({
          success: false,
          message: "Google account email is already registered",
        });
      }
      return res.status(409).json({
        success: false,
        message: "Username or email already exists",
      });
    }

    return res.status(500).json({
      success: false,
      message: "Family member registration failed",
    });
  }
}

async function getProfile(req, res) {
  try {
    const userId = req.user.id;
    const userProfile = await authService.getProfile(userId);

    if (!userProfile) {
      return res.status(404).json({
        success: false,
        message: "Account not found or inactive",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Profile retrieved successfully",
      data: userProfile,
    });
  } catch (error) {
    console.error("Get profile error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to retrieve user profile",
    });
  }
}

module.exports = {
  login,
  register,
  getProfile,
};
