const express = require("express");
const customerController = require("../controllers/customerController");
const { authenticateToken } = require("../middleware/authMiddleware");
const router = express.Router();

router.post("/", customerController.createCustomer);
router.get("/", customerController.getAllCustomers);
router.use(authenticateToken);
router.get("/:customerId", customerController.getCustomerById);
router.get(
  "/:customerId/balance",
  customerController.getCustomerBalance
);
module.exports = router;