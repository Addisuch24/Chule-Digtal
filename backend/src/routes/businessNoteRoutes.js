const express = require("express");
const businessNoteController = require("../controllers/businessNoteController");
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticateToken);

// Create global business note
router.post("/", businessNoteController.createBusinessNote);

// Get all global business notes
router.get("/", businessNoteController.getAllBusinessNotes);

module.exports = router;
