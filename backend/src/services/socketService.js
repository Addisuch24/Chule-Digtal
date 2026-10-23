let io = null;

function initializeSocket(socketIo) {
  io = socketIo;
}

function emitToUser(userId, event, data) {
  if (!io) {
    console.warn("Socket.IO has not been initialized");
    return;
  }

  io.to(`user:${userId}`).emit(event, data);
}

function emitToUsers(userIds, event, data) {
  if (!io) {
    console.warn("Socket.IO has not been initialized");
    return;
  }

  for (const userId of userIds) {
    io.to(`user:${userId}`).emit(event, data);
  }
}

function emitToAll(event, data) {
  if (!io) {
    console.warn("Socket.IO has not been initialized");
    return;
  }

  io.emit(event, data);
}

module.exports = {
  initializeSocket,
  emitToUser,
  emitToUsers,
  emitToAll,
};