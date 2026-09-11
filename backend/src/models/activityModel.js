const pool = require("../config/db");

async function createActivity({
  actorId,
  activityType,
  entityType,
  entityId,
  description,
  metadata,
}) {
  const [result] = await pool.execute(
    `
      INSERT INTO activities (
        actor_id,
        activity_type,
        entity_type,
        entity_id,
        description,
        metadata
      )
      VALUES (?, ?, ?, ?, ?, ?)
    `,
    [
      actorId,
      activityType,
      entityType,
      entityId,
      description || null,
      metadata ? JSON.stringify(metadata) : null,
    ]
  );

  return {
    id: result.insertId,
    actorId,
    activityType,
    entityType,
    entityId,
    description: description || null,
    metadata: metadata || null,
  };
}

async function getActivityById(activityId) {
  const [rows] = await pool.execute(
    `
      SELECT
        a.id,
        a.actor_id,
        fm.full_name AS actor_name,
        a.activity_type,
        a.entity_type,
        a.entity_id,
        a.description,
        a.metadata,
        a.created_at
      FROM activities a
      JOIN family_members fm
        ON fm.id = a.actor_id
      WHERE a.id = ?
    `,
    [activityId]
  );

  return rows[0] || null;
}

async function getAllActivities() {
  const [rows] = await pool.execute(
    `
      SELECT
        a.id,
        a.actor_id,
        fm.full_name AS actor_name,
        a.activity_type,
        a.entity_type,
        a.entity_id,
        a.description,
        a.metadata,
        a.created_at
      FROM activities a
      JOIN family_members fm
        ON fm.id = a.actor_id
      ORDER BY a.created_at DESC, a.id DESC
    `
  );

  return rows;
}

module.exports = {
  createActivity,
  getActivityById,
  getAllActivities,
};