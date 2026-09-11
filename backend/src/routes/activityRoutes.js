const express = require("express");
const activityController = require("../controllers/activityController");
const { authenticateToken } = require("../middleware/authMiddleware");
const router = express.Router();
router.use(authenticateToken);

// Create activity
router.post(
  "/",
  activityController.createActivity
);

// Get all activities
router.get(
  "/",
  activityController.getAllActivities
);

// Get one activity
router.get(
  "/:activityId",
  activityController.getActivityById
);

module.exports = router;