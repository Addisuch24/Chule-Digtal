const express = require("express");
const orderController = require("../controllers/orderController");
const { authenticateToken } = require("../middleware/authMiddleware");
const router = express.Router();
router.use(authenticateToken);
router.post("/", orderController.createOrder);
router.post(
  "/customers/:customerId/orders",
  orderController.createOrder
);
router.patch("/:orderId", orderController.updateOrder);
router.delete("/:orderId", orderController.deleteOrder);

router.get(
  "/orders/:orderId",
  orderController.getOrderById
);
router.get(
  "/",
  orderController.getAllOrders
);
router.get("/:orderId", orderController.getOrderById);
module.exports = router;