const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const compression = require("compression");
const notificationRoutes = require("./routes/notificationRoutes");
const customerRoutes = require("./routes/customerRoutes");
const customerNoteRoutes = require("./routes/customerNoteRoutes");
const businessNoteRoutes = require("./routes/businessNoteRoutes");
const orderRoutes = require("./routes/orderRoutes");
const financialMovementRoutes = require(
  "./routes/financialMovementRoutes"
);
const activityRoutes = require("./routes/activityRoutes");
const authRoutes = require("./routes/authRoutes");
const dashboardRoutes = require("./routes/dashboardRoutes");
const app = express();

app.use(helmet());
app.use(cors());
app.use(compression());
app.use(express.json());

// Safe development/staging diagnostic logger (redacts all sensitive payloads/headers)
app.use((req, res, next) => {
  const start = Date.now();
  const clientIp = req.ip || req.connection.remoteAddress;

  res.on("finish", () => {
    const duration = Date.now() - start;
    if (process.env.NODE_ENV !== "test") {
      console.log(`[HTTP ${res.statusCode}] ${req.method} ${req.originalUrl} (${duration}ms) from ${clientIp}`);
    }
  });

  next();
});

// Liveness health check
app.get("/api/health", (req, res) => {
  res.json({
    success: true,
    message: "Chule Digital API is running",
  });
});

// Readiness health check (verifies database dependency)
app.get("/api/health/ready", async (req, res) => {
  const pool = require("./config/db");
  let connection = null;
  try {
    connection = await pool.getConnection();
    await connection.query("SELECT 1");
    return res.status(200).json({
      success: true,
      message: "Database connection is ready",
    });
  } catch (error) {
    return res.status(503).json({
      success: false,
      message: "Database dependency is unavailable",
    });
  } finally {
    if (connection) {
      connection.release();
    }
  }
});

app.use("/api/activities", activityRoutes);
app.use("/api/customers", customerRoutes);
app.use("/api/customers", customerNoteRoutes);
app.use("/api/notes", businessNoteRoutes);
app.use("/api/orders", orderRoutes);
app.use(
  "/api/financial-movements",
  financialMovementRoutes
);
app.use("/api/notifications", notificationRoutes);
app.use("/api/auth", authRoutes);
app.use("/api/dashboard", dashboardRoutes);

module.exports = app;
