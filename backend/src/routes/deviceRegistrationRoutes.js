const express = require("express");
const { authenticateToken } = require("../middleware/authMiddleware");
const deviceRegistrationController = require("../controllers/deviceRegistrationController");

const router = express.Router();

// Register or update device push token (strictly requires authenticated user)
router.post("/register", authenticateToken, deviceRegistrationController.registerDevice);

// Disassociate user from device upon logout (retains logged-out delivery)
router.post("/logout", deviceRegistrationController.logoutDevice);

// Unregister / opt-out
router.delete("/:deviceToken", deviceRegistrationController.unregisterDevice);
router.post("/unregister", deviceRegistrationController.unregisterDevice);

module.exports = router;
