const notificationModel = require("../models/notificationModel");
const deviceRegistrationModel = require("../models/deviceRegistrationModel");
const socketService = require("./socketService");
const pushService = require("./pushService");
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

  // 1. Emit real-time Socket.IO event
  socketService.emitToUser(
    recipientId,
    "notification:new",
    payload
  );

  // 2. Dispatch push notification to registered devices (online/offline background delivery)
  try {
    const devices = await deviceRegistrationModel.getDevicesForUser(recipientId);
    const tokens = devices.map((d) => d.device_token);
    if (tokens.length > 0) {
      await pushService.sendPushNotification(tokens, payload);
    }
  } catch (err) {
    console.warn("[NOTIFICATION SERVICE] Error dispatching push to user devices:", err.message);
  }

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

  // Dispatch push notifications to all member devices
  try {
    for (let i = 0; i < recipientIds.length; i++) {
      const id = recipientIds[i];
      const notificationId = firstInsertId > 0 ? (firstInsertId + i) : null;
      const devices = await deviceRegistrationModel.getDevicesForUser(id);
      const tokens = devices.map((d) => d.device_token);
      if (tokens.length > 0) {
        await pushService.sendPushNotification(tokens, {
          id: notificationId,
          recipientId: Number(id),
          activityId: activityId ? Number(activityId) : null,
          entityType: entityType || null,
          entityId: entityId != null ? String(entityId) : null,
          notificationType,
          title: String(title || "Business Update"),
          message: String(message || ""),
          createdAt: nowIso,
        });
      }
    }
  } catch (err) {
    console.warn("[NOTIFICATION SERVICE] Error dispatching push to members:", err.message);
  }

  return [];
}

module.exports = {
  sendNotification,
  sendNotificationToMembers,
};
