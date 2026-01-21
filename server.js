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

// This function now correctly handles removing a user and destroying the room ONLY if it's empty.
const handleUserLeave = (socket) => {
  for (const roomCode in rooms) {
    const room = rooms[roomCode];
    const userIndex = room.users.indexOf(socket.id);

    if (userIndex !== -1) {
      console.log(`User ${socket.id} is leaving room '${roomCode}'`);
      // Remove the user from the room
      room.users.splice(userIndex, 1);

      // Notify remaining users that someone left
      socket.broadcast.to(roomCode).emit("system", "A user has left the chat.");

      // THE FINAL FIX: Only destroy the room if it's now empty.
      if (room.users.length === 0) {
        console.log(`Room '${roomCode}' is now empty and is being destroyed.`);
        delete rooms[roomCode];
      }
      break; // Exit loop once user is found and handled
    }
  }
};

io.on("connection", (socket) => {
  console.log(`A user connected: ${socket.id}`);

  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) { return; }
    rooms[roomCode] = {
      users: [],
      // THE FINAL FIX: Ensure maxUsers is always a number.
      maxUsers: parseInt(maxUsers, 10) || 2,
      endTime: Date.now() + duration,
    };
    console.log(`Room '${roomCode}' was created with a max of ${rooms[roomCode].maxUsers} users.`);
  });

  socket.on("join-room", (data) => {
    const roomCode = data.roomCode ? data.roomCode.trim() : '';
    if (!roomCode) { return; }
    const room = rooms[roomCode];
    if (!room) { return socket.emit("error-msg", "Room not found"); }

    // THE FINAL FIX: This comparison is now safe because maxUsers is a number.
    if (room.users.length >= room.maxUsers) {
      return socket.emit("error-msg", "Room is full");
    }
    if (Date.now() > room.endTime) { return socket.emit("error-msg", "Room has expired"); }

    room.users.push(socket.id);
    socket.join(roomCode);
    console.log(`Success: User ${socket.id} joined room '${roomCode}'. Current users: ${room.users.length}`);
    socket.emit("joined");
    socket.broadcast.to(roomCode).emit("system", "A user has joined the chat.");
  });

  // LEAVE ROOM - Now uses the new, safe logic.
  socket.on("leave-room", () => {
    handleUserLeave(socket);
  });

  socket.on("get-room-details", ({ roomCode }) => {
    const room = rooms[roomCode];
    if (room) {
      const remainingTime = Math.max(0, room.endTime - Date.now());
      socket.emit("room-details", { remainingTime });
    }
  });

  socket.on("send-message", ({ roomCode, message }) => {
    socket.broadcast.to(roomCode).emit("new-message", message);
  });

  // DISCONNECT - Now uses the new, safe logic.
  socket.on("disconnect", () => {
    console.log(`User disconnected: ${socket.id}`);
    handleUserLeave(socket);
  });
});

const PORT = process.env.PORT || 3000;

server.listen(PORT, "0.0.0.0", () => {
  console.log(`ANONX Server is running on port ${PORT}`);
});

