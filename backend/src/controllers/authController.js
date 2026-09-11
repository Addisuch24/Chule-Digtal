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
        message: "Invalid username or password",
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

module.exports = {
  login,
};