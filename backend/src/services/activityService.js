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

  const [members] = await pool.execute(
    `
      SELECT id
      FROM family_members
      WHERE is_active = TRUE
        AND id <> ?
    `,
    [actorId]
  );

  const recipientIds = members.map((member) => member.id);

  if (recipientIds.length > 0) {
    await notificationService.sendNotificationToMembers({
      recipientIds,
      activityId: activity.id,
      notificationType: activityType,
      title: activityType.replace(/_/g, " "),
      message: description || "A new business activity occurred.",
    });
  }

  return activity;
}

module.exports = {
  recordActivity,
};