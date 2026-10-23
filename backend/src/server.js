require("dotenv").config();
const jwt = require("jsonwebtoken");
const http = require("http");
const { Server } = require("socket.io");
const socketService = require("./services/socketService");
const app = require("./app");
const pool = require("./config/db");

const PORT = process.env.PORT || 5000;

async function connectWithRetry(maxRetries = 5, initialDelay = 1000) {
  let attempt = 0;
  let delay = initialDelay;

  while (attempt < maxRetries) {
    attempt++;
    let connection = null;
    try {
      connection = await pool.getConnection();
      console.log("Database connected successfully!");
      return true;
    } catch (error) {
      console.warn(`[DATABASE CONNECT ATTEMPT ${attempt}/${maxRetries} FAILED]: ${error.message}`);
      if (attempt >= maxRetries) {
        throw new Error(`Database connection failed after ${maxRetries} attempts: ${error.message}`);
      }
      console.log(`Retrying database connection in ${delay}ms...`);
      await new Promise((resolve) => setTimeout(resolve, delay));
      delay *= 2;
    } finally {
      if (connection) {
        connection.release();
      }
    }
  }
  return false;
}

async function startServer() {
  try {
    await connectWithRetry(5, 1000);

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

        const secret = process.env.JWT_SECRET || "chule_digita_change_this_to_a_long_random_secret";
        const decoded = jwt.verify(
          token,
          secret
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

    const serverInstance = httpServer.listen(PORT, "0.0.0.0", () => {
      console.log(
        `Chule Digital API is running on http://localhost:${PORT}`
      );
      console.log("Socket.IO real-time server is running");
    });

    // Graceful Shutdown
    const shutdownHandler = async (signal) => {
      console.log(`Received ${signal}. Shutting down gracefully...`);
      serverInstance.close(async () => {
        console.log("HTTP server closed.");
        try {
          await pool.end();
          console.log("Database connection pool closed.");
        } catch (poolErr) {
          console.error("Error closing database pool:", poolErr);
        }
        process.exit(0);
      });
    };

    process.on("SIGINT", () => shutdownHandler("SIGINT"));
    process.on("SIGTERM", () => shutdownHandler("SIGTERM"));

  } catch (error) {
    console.error("Fatal startup error:", error.message);
    console.error("Troubleshooting: Ensure MariaDB/MySQL is running on the configured host and port.");
    process.exit(1);
  }
}

startServer();
