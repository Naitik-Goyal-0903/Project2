const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
  cors: { origin: "*" }
});

// rooms = {
//   ROOM123: {
//     users: [socketId1, socketId2],
//     maxUsers: 2,
//     endTime: timestamp
//   }
// }
const rooms = {};

// 🔥 CLEAN & SAFE LEAVE HANDLER
function handleUserLeave(socket) {
  const roomCode = socket.data.roomCode;
  if (!roomCode || !rooms[roomCode]) return;

  const room = rooms[roomCode];

  room.users = room.users.filter(id => id !== socket.id);

  console.log(`User ${socket.id} left room ${roomCode}`);

  socket.broadcast.to(roomCode).emit("system", "A user left the chat");

  // destroy room only if empty
  if (room.users.length === 0) {
    console.log(`Room ${roomCode} destroyed (empty)`);
    delete rooms[roomCode];
  }

  delete socket.data.roomCode;
}

io.on("connection", (socket) => {
  console.log("User connected:", socket.id);

  // CREATE ROOM
  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) return;

    rooms[roomCode] = {
      users: [],
      maxUsers: parseInt(maxUsers, 10) || 2,
      endTime: Date.now() + (duration || 5 * 60 * 1000)
    };

    console.log(`Room created: ${roomCode}`);
  });

  // JOIN ROOM
  socket.on("join-room", ({ roomCode }) => {
    if (!roomCode) return;

    const room = rooms[roomCode];
    if (!room) {
      socket.emit("error-msg", "Room not found");
      return;
    }

    // prevent duplicate join
    if (room.users.includes(socket.id)) return;

    if (room.users.length >= room.maxUsers) {
      socket.emit("error-msg", "Room full");
      return;
    }

    if (Date.now() > room.endTime) {
      socket.emit("error-msg", "Room expired");
      return;
    }

    room.users.push(socket.id);
    socket.join(roomCode);
    socket.data.roomCode = roomCode;

    console.log(`User ${socket.id} joined ${roomCode}`);

    socket.emit("joined");
    socket.broadcast.to(roomCode).emit("system", "A user joined the chat");
  });

  // SEND MESSAGE (to everyone including sender)
  socket.on("send-message", ({ roomCode, message }) => {
    if (!roomCode || !message) return;
    io.to(roomCode).emit("new-message", message);
  });

  // MANUAL LEAVE
  socket.on("leave-room", () => {
    handleUserLeave(socket);
  });

  // DISCONNECT
  socket.on("disconnect", () => {
    console.log("User disconnected:", socket.id);
    handleUserLeave(socket);
  });
});

const PORT = 3000;
server.listen(PORT, "0.0.0.0", () => {
  console.log(`ANONX server running on port ${PORT}`);
});








