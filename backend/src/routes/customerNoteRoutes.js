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

// Bulk delete customer notes
router.post(
  "/:customerId/notes/bulk-delete",
  customerNoteController.deleteCustomerNotes
);
router.delete(
  "/:customerId/notes/bulk",
  customerNoteController.deleteCustomerNotes
);

// Delete single customer note
router.delete(
  "/:customerId/notes/:noteId",
  customerNoteController.deleteCustomerNote
);

module.exports = router;