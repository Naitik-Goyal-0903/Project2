const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
  cors: { origin: "*" }
});

const rooms = {};

function handleUserLeave(socket) {
  const roomCode = socket.data.roomCode;
  if (!roomCode || !rooms[roomCode]) return;

  const room = rooms[roomCode];
  room.users = room.users.filter(id => id !== socket.id);

  socket.broadcast.to(roomCode).emit("system", "A user left the chat");

  if (room.users.length === 0) {
    delete rooms[roomCode];
  }

  delete socket.data.roomCode;
}

io.on("connection", (socket) => {
  console.log("User connected:", socket.id);

  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) return;

    if (rooms[roomCode]) return; // ❌ overwrite avoid

    rooms[roomCode] = {
      users: [],
      maxUsers: parseInt(maxUsers, 10) || 2,
      endTime: Date.now() + (duration || 5 * 60 * 1000)
    };
  });

  socket.on("join-room", ({ roomCode }) => {
    const room = rooms[roomCode];
    if (!room) {
      socket.emit("error-msg", "Room not found");
      return;
    }

    if (room.users.length >= room.maxUsers) {
      socket.emit("error-msg", "Room full");
      return;
    }

    room.users.push(socket.id);
    socket.join(roomCode);
    socket.data.roomCode = roomCode;

    socket.emit("joined");
    socket.broadcast.to(roomCode).emit("system", "A user joined the chat");
  });

  // 🔥 FIXED MESSAGE SEND
  socket.on("send-message", ({ roomCode, message }) => {
    if (!roomCode || !message) return;

    // ❌ sender ko wapas mat bhejo
    socket.broadcast.to(roomCode).emit("new-message", message);
  });

  socket.on("leave-room", () => {
    handleUserLeave(socket);
  });

  socket.on("disconnect", () => {
    // ❌ ignore disconnect
  });
});

const PORT = 3000;
server.listen(PORT, "0.0.0.0", () => {
  console.log(`ANONX server running on port ${PORT}`);
});

