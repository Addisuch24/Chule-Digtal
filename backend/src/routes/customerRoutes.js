 const express = require("express");

const customerController = require("../controllers/customerController");
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticateToken);

// Create customer
router.post("/", customerController.createCustomer);

// Get all customers
router.get("/", customerController.getAllCustomers);

// Get customer balance
router.get(
  "/:customerId/balance",
  customerController.getCustomerBalance
);

// Bulk delete customers (support both POST and DELETE)
router.post("/bulk-delete", customerController.deleteCustomers);
router.delete("/bulk", customerController.deleteCustomers);

// Delete single customer
router.delete("/:customerId", customerController.deleteCustomer);

// Update customer
router.put("/:customerId", customerController.updateCustomer);
router.patch("/:customerId", customerController.updateCustomer);

// Get one customer
router.get(
  "/:customerId",
  customerController.getCustomerById
);

module.exports = router;