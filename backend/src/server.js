 require("dotenv").config();
const jwt = require("jsonwebtoken");
const http = require("http");
const { Server } = require("socket.io");
const socketService = require("./services/socketService");
const app = require("./app");
const pool = require("./config/db");

const PORT = process.env.PORT || 5001;

async function startServer() {
  try {
    const connection = await pool.getConnection();

    console.log("Database connected successfully!");

    connection.release();

    const httpServer = http.createServer(app);

    const io = new Server(httpServer, {
      cors: {
        origin: "*",
      },
    });

    socketService.initializeSocket(io);

  io.use((socket, next) => {
  try {
    const token = socket.handshake.auth.token;

    if (!token) {
      return next(
        new Error("Authentication token is required")
      );
    }

    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET
    );

    socket.user = {
      id: decoded.id,
      username: decoded.username,
    };

    next();
  } catch (error) {
    next(new Error("Invalid or expired token"));
  }
});

io.on("connection", (socket) => {
  const userId = socket.user.id;
  const roomName = `user:${userId}`;

  socket.join(roomName);

  console.log(
    `User ${userId} connected to Socket.IO: ${socket.id}`
  );

  socket.on("disconnect", () => {
    console.log(
      `User ${userId} disconnected: ${socket.id}`
    );
  });
});

    httpServer.listen(PORT, () => {
      console.log(
        `Chule Digita API is running on http://localhost:${PORT}`
      );
      console.log("Socket.IO real-time server is running");
    });
  } catch (error) {
    console.error(
      "Database connection failed:",
      error.message
    );

    process.exit(1);
  }
}

startServer();