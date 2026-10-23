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

router.get("/", orderController.getAllOrders);
router.get("/:orderId", orderController.getOrderById);

router.patch("/:orderId", orderController.updateOrder);
router.patch("/:orderId/status", orderController.updateOrderStatus);
router.patch("/:orderId/verify", orderController.verifyOrder);
router.patch("/:orderId/reject", orderController.rejectOrder);

// Bulk delete orders
router.post("/bulk-delete", orderController.deleteOrders);
router.delete("/bulk", orderController.deleteOrders);

router.delete("/:orderId", orderController.deleteOrder);

module.exports = router;
