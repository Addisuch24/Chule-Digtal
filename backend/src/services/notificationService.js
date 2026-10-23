const notificationModel = require("../models/notificationModel");
const socketService = require("./socketService");
const pool = require("../config/db");

async function sendNotification({
  recipientId,
  activityId,
  notificationType,
  title,
  message,
  entityType = null,
  entityId = null,
}) {
  const notification = await notificationModel.createNotification({
    recipientId,
    activityId,
    notificationType,
    title,
    message,
  });

  const nowIso = new Date().toISOString();
  const persistentId = Number(notification.id);

  const payload = {
    id: persistentId,
    recipientId: Number(recipientId),
    recipient_id: Number(recipientId),
    activityId: activityId ? Number(activityId) : null,
    activity_id: activityId ? Number(activityId) : null,
    entityType: entityType || null,
    entity_type: entityType || null,
    entityId: entityId != null ? String(entityId) : null,
    entity_id: entityId != null ? String(entityId) : null,
    notificationType,
    notification_type: notificationType,
    title: String(title || "Business Update"),
    message: String(message || ""),
    isRead: false,
    is_read: 0,
    createdAt: nowIso,
    created_at: nowIso,
  };

  socketService.emitToUser(
    recipientId,
    "notification:new",
    payload
  );

  return notification;
}

async function sendNotificationToMembers({
  recipientIds,
  activityId,
  notificationType,
  title,
  message,
  entityType = null,
  entityId = null,
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

  const [result] = await pool.query(
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

  const firstInsertId = Number(result.insertId);
  const nowIso = new Date().toISOString();

  // Emit socket events to connected sockets with verified persistent database IDs
  recipientIds.forEach((id, index) => {
    const notificationId = firstInsertId > 0 ? (firstInsertId + index) : null;
    const payload = {
      id: notificationId,
      recipientId: Number(id),
      recipient_id: Number(id),
      activityId: activityId ? Number(activityId) : null,
      activity_id: activityId ? Number(activityId) : null,
      entityType: entityType || null,
      entity_type: entityType || null,
      entityId: entityId != null ? String(entityId) : null,
      entity_id: entityId != null ? String(entityId) : null,
      notificationType,
      notification_type: notificationType,
      title: String(title || "Business Update"),
      message: String(message || ""),
      isRead: false,
      is_read: 0,
      createdAt: nowIso,
      created_at: nowIso,
    };

    socketService.emitToUser(id, "notification:new", payload);
  });

  return [];
}

module.exports = {
  sendNotification,
  sendNotificationToMembers,
};
