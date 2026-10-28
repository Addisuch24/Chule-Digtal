const pool = require("../config/db");

async function createNotification({
  recipientId,
  activityId,
  notificationType,
  title,
  message,
}) {
  const result = await pool.query(
    `
      INSERT INTO notifications (
        recipient_id,
        activity_id,
        notification_type,
        title,
        message
      )
      VALUES ($1, $2, $3, $4, $5)
      RETURNING id
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
    id: result.rows[0].id,
    recipientId,
    activityId: activityId || null,
    notificationType,
    title,
    message,
    isRead: false,
  };
}

async function getNotificationById(notificationId) {
  const result = await pool.query(
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
      WHERE n.id = $1
    `,
    [notificationId]
  );

  return result.rows[0] || null;
}

async function getNotificationsByRecipient(recipientId, options = {}) {
  const { unreadOnly = false, page = 1, limit = 50 } = options;
  const pageNum = Math.max(1, Number(page) || 1);
  const limitNum = Math.max(1, Math.min(Number(limit) || 50, 200));
  const offset = (pageNum - 1) * limitNum;

  let baseWhere = "WHERE n.recipient_id = $1";
  const whereParams = [recipientId];

  if (unreadOnly) {
    baseWhere += " AND n.is_read = FALSE";
  }

  // Count total notifications matching criteria
  const countQuery = `
    SELECT COUNT(*) AS total
    FROM notifications n
    ${baseWhere}
  `;
  const countResult = await pool.query(countQuery, whereParams);
  const total = Number(countResult.rows[0]?.total || 0);

  // Fetch paginated rows
  const query = `
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
    ${baseWhere}
    ORDER BY n.created_at DESC, n.id DESC
    LIMIT $${whereParams.length + 1} OFFSET $${whereParams.length + 2}
  `;
  const queryParams = [...whereParams, limitNum, offset];
  const result = await pool.query(query, queryParams);

  return {
    rows: result.rows,
    total,
    page: pageNum,
    limit: limitNum,
    totalPages: Math.max(1, Math.ceil(total / limitNum)),
  };
}

async function markNotificationAsRead(notificationId) {
  const result = await pool.query(
    `
      UPDATE notifications
      SET
        is_read = TRUE,
        read_at = CURRENT_TIMESTAMP
      WHERE id = $1
    `,
    [notificationId]
  );

  if (result.rowCount === 0) {
    return null;
  }

  return getNotificationById(notificationId);
}

async function markAllNotificationsAsRead(recipientId) {
  const result = await pool.query(
    `
      UPDATE notifications
      SET
        is_read = TRUE,
        read_at = CURRENT_TIMESTAMP
      WHERE recipient_id = $1 AND is_read = FALSE
    `,
    [recipientId]
  );
  return result.rowCount;
}

async function getUnreadNotificationCount(recipientId) {
  const result = await pool.query(
    `
      SELECT COUNT(*) AS unread_count
      FROM notifications
      WHERE recipient_id = $1
        AND is_read = FALSE
    `,
    [recipientId]
  );

  return Number(result.rows[0]?.unread_count || 0);
}

async function deleteNotification(notificationId, recipientId) {
  let query = "DELETE FROM notifications WHERE id = $1";
  const params = [notificationId];
  if (recipientId) {
    query += " AND recipient_id = $2";
    params.push(recipientId);
  }
  const result = await pool.query(query, params);
  return result.rowCount > 0;
}

async function deleteNotifications(notificationIds, recipientId) {
  if (!Array.isArray(notificationIds) || notificationIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skippedIds: [] };
  }

  const validIds = notificationIds.map(Number).filter((id) => Number.isInteger(id) && id > 0);
  if (validIds.length === 0) {
    return { deletedCount: 0, deletedIds: [], skippedIds: [] };
  }

  // Find which of these notifications actually exist and belong to the recipient
  let selectQuery = "SELECT id FROM notifications WHERE id = ANY($1::bigint[])";
  const selectParams = [validIds];
  if (recipientId) {
    selectQuery += " AND recipient_id = $2";
    selectParams.push(recipientId);
  }

  const existingRes = await pool.query(selectQuery, selectParams);
  const ownedIds = existingRes.rows.map((r) => Number(r.id));
  const skippedIds = validIds.filter((id) => !ownedIds.includes(id));

  if (ownedIds.length === 0) {
    return {
      deletedCount: 0,
      deletedIds: [],
      skippedIds,
    };
  }

  const deleteQuery = recipientId
    ? "DELETE FROM notifications WHERE id = ANY($1::bigint[]) AND recipient_id = $2"
    : "DELETE FROM notifications WHERE id = ANY($1::bigint[])";
  const deleteParams = recipientId ? [ownedIds, recipientId] : [ownedIds];

  const deleteRes = await pool.query(deleteQuery, deleteParams);

  return {
    deletedCount: deleteRes.rowCount,
    deletedIds: ownedIds,
    skippedIds,
  };
}

async function deleteAllNotifications(recipientId) {
  const result = await pool.query(
    "DELETE FROM notifications WHERE recipient_id = $1",
    [recipientId]
  );
  return result.rowCount;
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
  deleteAllNotifications,
};