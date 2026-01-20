const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
  cors: { origin: "*" },
  maxHttpBufferSize: 1e8, // 100 MB
});

const rooms = {};

const destroyRoom = (roomCode) => {
  if (rooms[roomCode]) {
    // Notify all clients in the room before destroying it
    io.to(roomCode).emit("room-destroyed");
    delete rooms[roomCode];
    console.log(`Room '${roomCode}' has been destroyed.`);
  }
};

io.on("connection", (socket) => {
  console.log(`A user connected: ${socket.id}`);

  // CREATE ROOM
  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) { return; }
    rooms[roomCode] = {
      users: [],
      maxUsers,
      endTime: Date.now() + duration,
    };
    console.log(`Room '${roomCode}' was created.`);
  });

  // JOIN ROOM
  socket.on("join-room", (data) => {
    const roomCode = data.roomCode ? data.roomCode.trim() : '';
    if (!roomCode) { return; }
    const room = rooms[roomCode];
    if (!room) { return socket.emit("error-msg", "Room not found"); }
    if (room.users.length >= room.maxUsers) { return socket.emit("error-msg", "Room is full"); }
    if (Date.now() > room.endTime) { return socket.emit("error-msg", "Room has expired"); }

    room.users.push(socket.id);
    socket.join(roomCode);
    console.log(`Success: User ${socket.id} joined room '${roomCode}'.`);
    socket.emit("joined");
    socket.broadcast.to(roomCode).emit("system", "A user has joined the chat.");
  });

  // LEAVE ROOM - NOW DESTROYS THE ROOM
  socket.on("leave-room", ({ roomCode }) => {
    console.log(`User ${socket.id} is leaving and destroying room '${roomCode}'`);
    destroyRoom(roomCode);
  });

  // GET ROOM DETAILS
  socket.on("get-room-details", ({ roomCode }) => {
    const room = rooms[roomCode];
    if (room) {
      const remainingTime = Math.max(0, room.endTime - Date.now());
      socket.emit("room-details", { remainingTime });
    }
  });

  // SEND MESSAGE
  socket.on("send-message", ({ roomCode, message }) => {
    socket.broadcast.to(roomCode).emit("new-message", message);
  });

  // DISCONNECT - NOW ALSO DESTROYS THE ROOM
  socket.on("disconnect", () => {
    console.log(`User disconnected: ${socket.id}`);
    for (const roomCode in rooms) {
      const room = rooms[roomCode];
      const userIndex = room.users.indexOf(socket.id);
      if (userIndex !== -1) {
        console.log(`Disconnected user was in room '${roomCode}'. Destroying room.`);
        destroyRoom(roomCode);
        break;
      }
    }
  });
});

server.listen(3000, "0.0.0.0", () => {
  console.log("ANONX Server is running on port 3000");
});
