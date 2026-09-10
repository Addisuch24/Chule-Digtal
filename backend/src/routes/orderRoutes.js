const express = require("express");
const orderController = require("../controllers/orderController");

const router = express.Router();

router.post(
  "/customers/:customerId/orders",
  orderController.createOrder
);

router.get(
  "/orders/:orderId",
  orderController.getOrderById
);
router.get(
  "/",
  orderController.getAllOrders
);
router.patch(
  "/:orderId/verify",
  orderController.verifyOrder
);
router.patch(
  "/:orderId/reject",
  orderController.rejectOrder
);
router.patch(
  "/:orderId/status",
  orderController.updateOrderStatus
);
module.exports = router;