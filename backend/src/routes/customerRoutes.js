const express = require("express");
const customerController = require("../controllers/customerController");

const router = express.Router();

router.post("/", customerController.createCustomer);
router.get("/", customerController.getAllCustomers);
router.get("/:customerId", customerController.getCustomerById);
router.get(
  "/:customerId/balance",
  customerController.getCustomerBalance
);
module.exports = router;