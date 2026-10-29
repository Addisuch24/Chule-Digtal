const express = require("express");
const financialMovementController = require(
  "../controllers/financialMovementController"
);
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();
router.use(authenticateToken);
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

// Bulk delete financial movements
router.post(
  "/bulk-delete",
  financialMovementController.deleteFinancialMovements
);
router.delete(
  "/bulk",
  financialMovementController.deleteFinancialMovements
);

// Update or delete a financial movement
router.patch(
  "/:movementId",
  financialMovementController.updateFinancialMovement
);
router.put(
  "/:movementId",
  financialMovementController.updateFinancialMovement
);
router.delete(
  "/:movementId",
  financialMovementController.deleteFinancialMovement
);

// Verify financial movement
router.patch(
  "/:movementId/verify",
  financialMovementController.verifyFinancialMovement
);
router.post(
  "/:movementId/verify",
  financialMovementController.verifyFinancialMovement
);
// Reject financial movement
router.patch(
  "/:movementId/reject",
  financialMovementController.rejectFinancialMovement
);
router.post(
  "/:movementId/reject",
  financialMovementController.rejectFinancialMovement
);
// Record customer payment
router.post(
  "/payments",
  financialMovementController.createPayment
);
router.get(
  "/customer/:customerId/payments",
  financialMovementController.getCustomerPayments
);
// Get one financial movement
router.get(
  "/:movementId",
  financialMovementController.getFinancialMovementById
);


module.exports = router;