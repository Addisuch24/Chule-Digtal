const pool = require("../config/db");

async function createNotification({
  recipientId,
  activityId,
  notificationType,
  title,
  message,
}) {
  const [result] = await pool.execute(
    `
      INSERT INTO notifications (
        recipient_id,
        activity_id,
        notification_type,
        title,
        message
      )
      VALUES (?, ?, ?, ?, ?)
    `,
    [
      recipientId,
      activityId || null,
      notificationType,
      title,
      message,
    ]
  );

  return {
    id: result.insertId,
    recipientId,
    activityId: activityId || null,
    notificationType,
    title,
    message,
    isRead: false,
  };
}

async function getNotificationById(notificationId) {
  const [rows] = await pool.execute(
    `
      SELECT
        n.id,
        n.recipient_id,
        fm.full_name AS recipient_name,
        n.activity_id,
        n.notification_type,
        n.title,
        n.message,
        n.is_read,
        n.read_at,
        n.created_at
      FROM notifications n
      JOIN family_members fm
        ON fm.id = n.recipient_id
      WHERE n.id = ?
    `,
    [notificationId]
  );

  return rows[0] || null;
}

async function getNotificationsByRecipient(recipientId, options = {}) {
  const { unreadOnly = false, limit = 100 } = options;
  let query = `
    SELECT
      n.id,
      n.recipient_id,
      n.activity_id,
      n.notification_type,
      n.title,
      n.message,
      n.is_read,
      n.read_at,
      n.created_at,
      a.entity_type,
      a.entity_id
    FROM notifications n
    LEFT JOIN activities a
      ON a.id = n.activity_id
    WHERE n.recipient_id = ?
  `;
  const params = [recipientId];

  if (unreadOnly) {
    query += " AND n.is_read = FALSE";
  }

  query += " ORDER BY n.created_at DESC, n.id DESC LIMIT ?";
  params.push(Math.max(1, Math.min(Number(limit) || 100, 200)));

  const [rows] = await pool.query(query, params);
  return rows;
}

async function markNotificationAsRead(notificationId) {
  const [result] = await pool.execute(
    `
      UPDATE notifications
      SET
        is_read = TRUE,
        read_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `,
    [notificationId]
  );

  if (result.affectedRows === 0) {
    return null;
  }

  return getNotificationById(notificationId);
}

async function markAllNotificationsAsRead(recipientId) {
  const [result] = await pool.execute(
    `
      UPDATE notifications
      SET
        is_read = TRUE,
        read_at = CURRENT_TIMESTAMP
      WHERE recipient_id = ? AND is_read = FALSE
    `,
    [recipientId]
  );
  return result.affectedRows;
}

async function getUnreadNotificationCount(recipientId) {
  const [rows] = await pool.execute(
    `
      SELECT COUNT(*) AS unread_count
      FROM notifications
      WHERE recipient_id = ?
        AND is_read = FALSE
    `,
    [recipientId]
  );

  return Number(rows[0].unread_count);
}
async function deleteNotification(notificationId, recipientId) {
  let query = "DELETE FROM notifications WHERE id = ?";
  const params = [notificationId];
  if (recipientId) {
    query += " AND recipient_id = ?";
    params.push(recipientId);
  }
  const [result] = await pool.execute(query, params);
  return result.affectedRows > 0;
}

async function deleteNotifications(notificationIds, recipientId) {
  if (!Array.isArray(notificationIds) || notificationIds.length === 0) {
    return { deletedCount: 0, deletedIds: [] };
  }
  let query = "DELETE FROM notifications WHERE id IN (?)";
  const params = [notificationIds];
  if (recipientId) {
    query += " AND recipient_id = ?";
    params.push(recipientId);
  }
  const [result] = await pool.query(query, params);
  return {
    deletedCount: result.affectedRows,
    deletedIds: notificationIds,
  };
}

module.exports = {
  createNotification,
  getNotificationById,
  getNotificationsByRecipient,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  getUnreadNotificationCount,
  deleteNotification,
  deleteNotifications,
};