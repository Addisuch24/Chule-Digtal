const express = require("express");
const dashboardController = require("../controllers/dashboardController");
const { authenticateToken } = require("../middleware/authMiddleware");

const router = express.Router();

router.use(authenticateToken);

router.get("/", dashboardController.getDashboardSummary);

module.exports = router;