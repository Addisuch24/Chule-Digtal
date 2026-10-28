const activityModel = require("../models/activityModel");
const notificationService = require("./notificationService");

async function recordActivity({
  actorId,
  activityType,
  entityType,
  entityId,
  description,
  metadata,
}) {
  const activity = await activityModel.createActivity({
    actorId,
    activityType,
    entityType,
    entityId,
    description,
    metadata,
  });

  // Get all active family members except the actor
  const pool = require("../config/db");

  const members = await pool.query(
    `
      SELECT id
      FROM family_members
      WHERE is_active = 1
        AND username NOT LIKE 'test_%'
        AND username NOT LIKE 'integ_test_%'
        AND username NOT LIKE 'audit_user_%'
        AND username NOT LIKE 'realtime_user_%'
        AND username NOT LIKE 'ord_%'
        AND username NOT LIKE 'debug_%'
        AND username NOT LIKE '%test%'
        AND id <> $1
    `,
    [actorId]
  );

  const recipientIds = members.rows.map((member) => member.id);

  if (recipientIds.length > 0) {
    await notificationService.sendNotificationToMembers({
      recipientIds,
      activityId: activity.id,
      notificationType: activityType,
      title: activityType.replace(/_/g, " "),
      message: description || "A new business activity occurred.",
      entityType,
      entityId,
    });
  }

  const socketService = require("./socketService");
  socketService.emitToAll("dashboard:refresh", {
    activityType,
    entityType,
    entityId,
    timestamp: new Date().toISOString(),
  });

  return activity;
}

module.exports = {
  recordActivity,
};