const notificationModel = require("../models/notificationModel");
const socketService = require("./socketService");

async function sendNotification({
  recipientId,
  activityId,
  notificationType,
  title,
  message,
}) {
  const notification =
    await notificationModel.createNotification({
      recipientId,
      activityId,
      notificationType,
      title,
      message,
    });

  socketService.emitToUser(
    recipientId,
    "notification:new",
    notification
  );

  return notification;
}

async function sendNotificationToMembers({
  recipientIds,
  activityId,
  notificationType,
  title,
  message,
}) {
  const notifications = [];

  for (const recipientId of recipientIds) {
    const notification = await sendNotification({
      recipientId,
      activityId,
      notificationType,
      title,
      message,
    });

    notifications.push(notification);
  }

  return notifications;
}

module.exports = {
  sendNotification,
  sendNotificationToMembers,
};