const jwt = require("jsonwebtoken");
const { getJwtSecret } = require("../config/env");

function authenticateToken(req, res, next) {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
      return res.status(401).json({
        success: false,
        message: "Authorization header is required",
      });
    }

    const parts = authHeader.split(" ");

    if (parts.length !== 2 || parts[0] !== "Bearer") {
      return res.status(401).json({
        success: false,
        message: "Invalid authorization format",
      });
    }

    const token = parts[1];
    const secret = getJwtSecret();

    const decoded = jwt.verify(token, secret);

    req.user = {
      id: decoded.id ?? decoded.userId,
      username: decoded.username,
    };

    next();
  } catch (error) {
    console.warn(`[AUTH MIDDLEWARE DIAGNOSTIC] Token verification failed: ${error.message}`);
    return res.status(401).json({
      success: false,
      message: "Invalid or expired token",
    });
  }
}

module.exports = {
  authenticateToken,
};
