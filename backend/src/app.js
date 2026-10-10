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

app.get("/api/health", (req, res) => {
  res.json({
    success: true,
    message: "Chule Digita API is running",
  });
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
