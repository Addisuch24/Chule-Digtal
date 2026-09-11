const express = require("express");
const notificationController = require("../controllers/notificationController");
const { authenticateToken } = require("../middleware/authMiddleware");
const router = express.Router();
router.use(authenticateToken);

router.post(
  "/",
  notificationController.createNotification
);

router.get(
  "/recipient/:recipientId",
  notificationController.getNotificationsByRecipient
);
router.patch(
  "/:notificationId/read",
  notificationController.markNotificationAsRead
);
router.get(
  "/recipient/:recipientId/unread-count",
  notificationController.getUnreadNotificationCount
);
router.get(
  "/:notificationId",
  notificationController.getNotificationById
);

module.exports = router;