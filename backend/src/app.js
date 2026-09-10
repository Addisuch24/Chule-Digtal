const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const compression = require("compression");

const customerRoutes = require("./routes/customerRoutes");
const customerNoteRoutes = require("./routes/customerNoteRoutes");
const orderRoutes = require("./routes/orderRoutes");
const financialMovementRoutes = require(
  "./routes/financialMovementRoutes"
);
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

app.use("/api/customers", customerRoutes);
app.use("/api/customers", customerNoteRoutes);
app.use("/api/orders", orderRoutes);
app.use(
  "/api/financial-movements",
  financialMovementRoutes
);
module.exports = app;