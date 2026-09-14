const notificationModel = require("../models/notificationModel");

async function createNotification(req, res) {
  try {
    const {
      recipientId,
      activityId,
      notificationType,
      title,
      message,
    } = req.body;

    if (!recipientId) {
      return res.status(400).json({
        success: false,
        message: "recipientId is required",
      });
    }

    if (!notificationType) {
      return res.status(400).json({
        success: false,
        message: "notificationType is required",
      });
    }

    if (!title) {
      return res.status(400).json({
        success: false,
        message: "title is required",
      });
    }

    if (!message) {
      return res.status(400).json({
        success: false,
        message: "message is required",
      });
    }

    const notification =
      await notificationModel.createNotification({
        recipientId: Number(recipientId),
        activityId: activityId
          ? Number(activityId)
          : null,
        notificationType,
        title,
        message,
      });

    return res.status(201).json({
      success: true,
      message: "Notification created successfully",
      data: notification,
    });
  } catch (error) {
    console.error("Create notification error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to create notification",
    });
  }
}
async function getNotificationById(req, res) {
  try {
     const { notificationId } = req.params;


    if (!notificationId || isNaN(notificationId)) {
      return res.status(400).json({
        success: false,
        message: "Valid notificationId is required",
      });
    }

    const notification =
      await notificationModel.getNotificationById(
        Number(notificationId)
      );

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: "Notification not found",
      });
    }

    return res.status(200).json({
      success: true,
      data: notification,
    });
  } catch (error) {
    console.error("Get notification error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to get notification",
    });
  }
}
async function getNotificationsByRecipient(req, res) {
  try {
 const recipientId = req.user.id;
    if (!recipientId || isNaN(recipientId)) {
      return res.status(400).json({
        success: false,
        message: "Valid recipientId is required",
      });
    }

    const notifications =
      await notificationModel.getNotificationsByRecipient(
        Number(recipientId)
      );

    return res.status(200).json({
      success: true,
      data: notifications,
    });
  } catch (error) {
    console.error(
      "Get recipient notifications error:",
      error
    );

    return res.status(500).json({
      success: false,
      message: "Failed to get notifications",
    });
  }
}
async function markNotificationAsRead(req, res) {
  try {
    const { notificationId } = req.params;

    if (!notificationId || isNaN(notificationId)) {
      return res.status(400).json({
        success: false,
        message: "Valid notificationId is required",
      });
    }
    const existingNotification =
      await notificationModel.getNotificationById(notificationId);

if (!existingNotification) {
  return res.status(404).json({
    success: false,
    message: "Notification not found",
  });
}

if (existingNotification.recipient_id !== req.user.id) {
  return res.status(403).json({
    success: false,
    message: "You are not allowed to modify this notification",
  });
}

    const notification =
      await notificationModel.markNotificationAsRead(
        Number(notificationId)
      );

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: "Notification not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Notification marked as read",
      data: notification,
    });
  } catch (error) {
    console.error("Mark notification as read error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to mark notification as read",
    });
  }
}
async function getUnreadNotificationCount(req, res) {
  try {
    const recipientId = req.user.id;
    if (!recipientId || isNaN(recipientId)) {
      return res.status(400).json({
        success: false,
        message: "Valid recipientId is required",
      });
    }

    const unreadCount =
      await notificationModel.getUnreadNotificationCount(
        Number(recipientId)
      );

    return res.status(200).json({
      success: true,
      data: {
        unreadCount,
      },
    });
  } catch (error) {
    console.error(
      "Get unread notification count error:",
      error
    );

    return res.status(500).json({
      success: false,
      message: "Failed to get unread notification count",
    });
  }
}
module.exports = {
  createNotification,
  getNotificationById,
  getNotificationsByRecipient,
  markNotificationAsRead,
  getUnreadNotificationCount,
};