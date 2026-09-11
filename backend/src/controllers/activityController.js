const activityModel = require("../models/activityModel");

async function createActivity(req, res) {
  try {
    const {
       activityType,
      entityType,
      entityId,
      description,
      metadata,
    } = req.body;
const actorId = req.user.id;
    if (
      !Number.isInteger(Number(actorId)) ||
      Number(actorId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "actorId must be a valid family member ID",
      });
    }

    if (!activityType || !entityType) {
      return res.status(400).json({
        success: false,
        message: "activityType and entityType are required",
      });
    }

    if (
      !Number.isInteger(Number(entityId)) ||
      Number(entityId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "entityId must be a valid ID",
      });
    }

    const activity = await activityModel.createActivity({
      actorId: Number(actorId),
      activityType,
      entityType,
      entityId: Number(entityId),
      description,
      metadata,
    });

    return res.status(201).json({
      success: true,
      message: "Activity recorded successfully",
      data: activity,
    });
  } catch (error) {
    console.error("Create activity error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to record activity",
    });
  }
}

async function getActivityById(req, res) {
  try {
    const { activityId } = req.params;

    if (
      !Number.isInteger(Number(activityId)) ||
      Number(activityId) <= 0
    ) {
      return res.status(400).json({
        success: false,
        message: "activityId must be a valid activity ID",
      });
    }

    const activity = await activityModel.getActivityById(
      Number(activityId)
    );

    if (!activity) {
      return res.status(404).json({
        success: false,
        message: "Activity not found",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Activity retrieved successfully",
      data: activity,
    });
  } catch (error) {
    console.error("Get activity error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve activity",
    });
  }
}

async function getAllActivities(req, res) {
  try {
    const activities = await activityModel.getAllActivities();

    return res.status(200).json({
      success: true,
      message: "Activities retrieved successfully",
      data: activities,
    });
  } catch (error) {
    console.error("Get activities error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to retrieve activities",
    });
  }
}

module.exports = {
  createActivity,
  getActivityById,
  getAllActivities,
};