const express = require("express");
const customerNoteController = require("../controllers/customerNoteController");

const router = express.Router();

router.post(
  "/:customerId/notes",
  customerNoteController.createCustomerNote
);
router.get(
  "/:customerId/notes",
  customerNoteController.getCustomerNotes
);

module.exports = router;