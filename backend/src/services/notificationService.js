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
  const placeholders = [];
  const flatParams = [];
  recipientIds.forEach((id) => {
    const start = flatParams.length;
    placeholders.push(`($${start + 1}, $${start + 2}, $${start + 3}, $${start + 4}, $${start + 5})`);
    flatParams.push(id, activityId || null, notificationType, title, message);
  });

  const insertSql = `
    INSERT INTO notifications (
      recipient_id,
      activity_id,
      notification_type,
      title,
      message
    )
    VALUES ${placeholders.join(", ")}
    RETURNING id
  `;

  const insertRes = await pool.query(insertSql, flatParams);
  const insertedIds = insertRes.rows.map((r) => Number(r.id));
  const nowIso = new Date().toISOString();

  // Emit socket events to connected sockets with verified persistent database IDs
  recipientIds.forEach((id, index) => {
    const notificationId = insertedIds[index] || null;
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

  // Dispatch push notifications to registered member devices in a single batch query
  try {
    const devicesRes = await pool.query(
      "SELECT device_token, user_id FROM device_registrations WHERE user_id = ANY($1::bigint[])",
      [recipientIds]
    );

    if (devicesRes.rows.length > 0) {
      const tokenMap = new Map();
      for (const row of devicesRes.rows) {
        const uid = Number(row.user_id);
        if (!tokenMap.has(uid)) tokenMap.set(uid, []);
        tokenMap.get(uid).push(row.device_token);
      }

      for (let i = 0; i < recipientIds.length; i++) {
        const id = Number(recipientIds[i]);
        const tokens = tokenMap.get(id);
        if (tokens && tokens.length > 0) {
          const notificationId = insertedIds[i] || null;
          await pushService.sendPushNotification(tokens, {
            id: notificationId,
            recipientId: id,
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
