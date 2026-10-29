const http = require("http");
const app = require("../src/app");
const { Server } = require("socket.io");
const socketService = require("../src/services/socketService");

const jwt = require("jsonwebtoken");
const { getJwtSecret } = require("../src/config/env");

let activeServer = null;

async function ensureTestServerRunning(port = 5055) {
  try {
    const res = await fetch(`http://localhost:${port}/api/health`);
    if (res.ok) {
      return { created: false, url: `http://localhost:${port}` };
    }
  } catch (err) {
    // Server not running, spin it up
  }

  if (activeServer) {
    return { created: false, url: `http://localhost:${port}` };
  }

  const httpServer = http.createServer(app);
  const io = new Server(httpServer, {
    cors: {
      origin: "*",
      credentials: true,
    },
  });

  io.use((socket, next) => {
    try {
      const token = socket.handshake.auth.token;
      if (!token) {
        return next(new Error("Authentication token is required"));
      }

      const secret = getJwtSecret();
      const decoded = jwt.verify(token, secret);
      socket.user = {
        id: decoded.id ?? decoded.userId,
        username: decoded.username,
      };
      next();
    } catch (err) {
      next(new Error("Invalid or expired token"));
    }
  });

  io.on("connection", (socket) => {
    const userId = socket.user?.id;
    if (userId) {
      socket.join(`user:${userId}`);
    }
  });

  socketService.initializeSocket(io);

  await new Promise((resolve, reject) => {
    httpServer.listen(port, "localhost", () => resolve());
    httpServer.on("error", reject);
  });

  activeServer = httpServer;
  return { created: true, url: `http://localhost:${port}` };
}

async function stopTestServer() {
  if (activeServer) {
    await new Promise((resolve) => activeServer.close(resolve));
    activeServer = null;
  }
}

module.exports = {
  ensureTestServerRunning,
  stopTestServer,
};
