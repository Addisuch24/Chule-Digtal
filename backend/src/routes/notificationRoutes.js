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
  "/",
  notificationController.getNotificationsByRecipient
);
router.get(
  "/unread",
  (req, res, next) => {
    req.query.unread = "true";
    return notificationController.getNotificationsByRecipient(req, res, next);
  }
);
router.get(
  "/unread-count",
  notificationController.getUnreadNotificationCount
);
router.patch(
  "/read-all",
  notificationController.markAllNotificationsAsRead
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
router.post("/bulk-delete", notificationController.deleteNotifications);
router.delete("/bulk", notificationController.deleteNotifications);
router.delete("/:notificationId", notificationController.deleteNotification);

router.get(
  "/:notificationId",
  notificationController.getNotificationById
);

module.exports = router;