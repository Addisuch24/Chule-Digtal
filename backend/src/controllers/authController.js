const authService = require("../services/authService");

async function login(req, res) {
  try {
    const { username, password } = req.body;

    if (!username) {
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
      username,
      password
    );

    if (!result) {
      return res.status(401).json({
        success: false,
        message:
          "Invalid username or password",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Login successful",
      data: result,
    });
  } catch (error) {
    console.error("Login error:", error);

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
      username,
      password,
    } = req.body;

    if (!fullName) {
      return res.status(400).json({
        success: false,
        message: "Full name is required",
      });
    }

    if (!username) {
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
        message:
          "Password must be at least 4 characters",
      });
    }

    const result =
      await authService.register(
        fullName.trim(),
        phone?.trim() || null,
        username.trim(),
        password
      );

    return res.status(201).json({
      success: true,
      message:
        "Family member registered successfully",
      data: result,
    });
  } catch (error) {
    console.error(
      "Registration error:",
      error
    );

    if (
      error.code ===
      "USERNAME_EXISTS"
    ) {
      return res.status(409).json({
        success: false,
        message: "Username already exists",
      });
    }

    if (error.code === "ER_DUP_ENTRY") {
      return res.status(409).json({
        success: false,
        message: "Username already exists",
      });
    }

    return res.status(500).json({
      success: false,
      message:
        "Family member registration failed",
    });
  }
}

module.exports = {
  login,
  register,
};