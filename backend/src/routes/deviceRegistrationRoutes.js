const express = require("express");
const jwt = require("jsonwebtoken");
const { getJwtSecret } = require("../config/env");
const deviceRegistrationController = require("../controllers/deviceRegistrationController");

const router = express.Router();

function optionalAuthenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    req.user = null;
    return next();
  }

  const parts = authHeader.split(" ");
  if (parts.length === 2 && parts[0] === "Bearer") {
    try {
      const secret = getJwtSecret();
      const decoded = jwt.verify(parts[1], secret);
      req.user = {
        id: decoded.id ?? decoded.userId,
        username: decoded.username,
      };
    } catch (e) {
      req.user = null;
    }
  }
  next();
}

// Register or update device push token (logged in or logged out)
router.post("/register", optionalAuthenticate, deviceRegistrationController.registerDevice);

// Disassociate user from device upon logout (retains logged-out delivery)
router.post("/logout", deviceRegistrationController.logoutDevice);

// Unregister / opt-out
router.delete("/:deviceToken", deviceRegistrationController.unregisterDevice);
router.post("/unregister", deviceRegistrationController.unregisterDevice);

module.exports = router;
