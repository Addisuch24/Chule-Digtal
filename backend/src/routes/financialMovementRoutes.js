const express = require("express");
const financialMovementController = require(
  "../controllers/financialMovementController"
);

const router = express.Router();

// Create financial movement
router.post(
  "/",
  financialMovementController.createFinancialMovement
);

// Get all financial movements
router.get(
  "/",
  financialMovementController.getAllFinancialMovements
);

// Verify financial movement
router.patch(
  "/:movementId/verify",
  financialMovementController.verifyFinancialMovement
);
// Reject financial movement
router.patch(
  "/:movementId/reject",
  financialMovementController.rejectFinancialMovement
);
router.get(
  "/:movementId",
  financialMovementController.getFinancialMovementById
);
// Get one financial movement
router.get(
  "/:movementId",
  financialMovementController.getFinancialMovementById
);

module.exports = router;