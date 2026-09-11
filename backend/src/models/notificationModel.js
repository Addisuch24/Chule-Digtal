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

async function getNotificationsByRecipient(recipientId) {
  const [rows] = await pool.execute(
    `
      SELECT
        n.id,
        n.recipient_id,
        n.activity_id,
        n.notification_type,
        n.title,
        n.message,
        n.is_read,
        n.read_at,
        n.created_at
      FROM notifications n
      WHERE n.recipient_id = ?
      ORDER BY n.created_at DESC, n.id DESC
    `,
    [recipientId]
  );

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
module.exports = {
  createNotification,
  getNotificationById,
  getNotificationsByRecipient,
  markNotificationAsRead,
  getUnreadNotificationCount,
};