const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);

// THE FINAL, ONE-GO FIX: Forcing a stable transport method for Render
const io = new Server(server, {
  cors: { origin: "*" },
  maxHttpBufferSize: 1e8,
  // This tells the server to prefer the ultra-stable polling method.
  transports: ["polling", "websocket"],
  allowEIO3: true, // Keep for maximum compatibility
});

const rooms = {};

const handleUserLeave = (socket) => {
  const roomCode = socket.roomCode;
  if (!roomCode || !rooms[roomCode]) {
    return;
  }
  const room = rooms[roomCode];
  const userIndex = room.users.indexOf(socket.id);
  if (userIndex !== -1) {
    room.users.splice(userIndex, 1);
    if (room.users.length === 0) {
      delete rooms[roomCode];
    } else {
      socket.broadcast.to(roomCode).emit("system", "A user has left the chat.");
    }
  }
};

io.on("connection", (socket) => {
  console.log(`A user connected: ${socket.id} via ${socket.conn.transport.name}`);

  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) { return; }
    rooms[roomCode] = {
      users: [],
      maxUsers: parseInt(maxUsers, 10) || 2,
      endTime: Date.now() + duration,
    };
  });

  socket.on("join-room", (data) => {
    const roomCode = data.roomCode ? data.roomCode.trim() : '';
    if (!roomCode) { return; }
    const room = rooms[roomCode];
    if (!room) { return socket.emit("error-msg", "Room not found"); }
    if (room.users.length >= room.maxUsers) {
      return socket.emit("error-msg", "Room is full");
    }
    if (Date.now() > room.endTime) { return socket.emit("error-msg", "Room has expired"); }

    socket.roomCode = roomCode;
    room.users.push(socket.id);
    socket.join(roomCode);

    socket.emit("joined");
    socket.broadcast.to(roomCode).emit("system", "A user has joined the chat.");
  });

  socket.on("leave-room", () => {
    handleUserLeave(socket);
  });

  socket.on("get-room-details", ({ roomCode }) => {
    const room = rooms[roomCode];
    if (room) {
      socket.emit("room-details", { remainingTime: Math.max(0, room.endTime - Date.now()) });
    }
  });

  socket.on("send-message", ({ roomCode, message }) => {
    socket.broadcast.to(roomCode).emit("new-message", message);
  });

  socket.on("disconnect", () => {
    handleUserLeave(socket);
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, "0.0.0.0", () => {
  console.log(`ANONX Server is running on port ${PORT}`);
});






