const express = require("express");
const businessNoteController = require("../controllers/businessNoteController");
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticateToken);

// Create global business note
router.post("/", businessNoteController.createBusinessNote);

// Get all global business notes
router.get("/", businessNoteController.getAllBusinessNotes);

// Bulk delete business notes
router.post("/bulk-delete", businessNoteController.deleteBusinessNotes);
router.delete("/bulk", businessNoteController.deleteBusinessNotes);

router.patch("/:noteId", businessNoteController.updateBusinessNote);
router.delete("/:noteId", businessNoteController.deleteBusinessNote);

module.exports = router;
