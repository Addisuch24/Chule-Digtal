const notificationModel = require("../models/notificationModel");
const socketService = require("./socketService");
const pool = require("../config/db");

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
  if (!recipientIds || recipientIds.length === 0) return [];

  // Batch insert all member notifications in a single fast query
  const values = recipientIds.map((id) => [
    id,
    activityId || null,
    notificationType,
    title,
    message,
  ]);

  await pool.query(
    `
      INSERT INTO notifications (
        recipient_id,
        activity_id,
        notification_type,
        title,
        message
      )
      VALUES ?
    `,
    [values]
  );

  // Emit socket events to connected sockets
  recipientIds.forEach((id) => {
    socketService.emitToUser(id, "notification:new", {
      activityId: activityId || null,
      notificationType,
      title,
      message,
      isRead: false,
    });
  });

  return [];
}

module.exports = {
  sendNotification,
  sendNotificationToMembers,
};
