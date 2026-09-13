const express = require("express");

const customerNoteController = require("../controllers/customerNoteController");
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticateToken);

// Create customer note
router.post(
  "/:customerId/notes",
  customerNoteController.createCustomerNote
);

// Get customer notes
router.get(
  "/:customerId/notes",
  customerNoteController.getCustomerNotes
);

module.exports = router;