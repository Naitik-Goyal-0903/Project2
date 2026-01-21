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

// A NEW, SIMPLER, AND SAFER WAY TO HANDLE USERS LEAVING.
const handleUserLeave = (socket) => {
  const roomCode = socket.roomCode;
  if (!roomCode || !rooms[roomCode]) {
    // This user wasn't in a room, or the room is already gone.
    return;
  }

  const room = rooms[roomCode];
  const userIndex = room.users.indexOf(socket.id);

  if (userIndex !== -1) {
    room.users.splice(userIndex, 1);
    console.log(`User ${socket.id} has left room '${roomCode}'.`);

    // Only destroy the room if it has become empty.
    if (room.users.length === 0) {
      console.log(`Room '${roomCode}' is empty, destroying.`);
      delete rooms[roomCode];
    } else {
      // Notify the remaining users.
      socket.broadcast.to(roomCode).emit("system", "A user has left the chat.");
    }
  }
};

io.on("connection", (socket) => {
  console.log(`A user connected: ${socket.id}`);

  socket.on("create-room", ({ roomCode, maxUsers, duration }) => {
    if (!roomCode) { return; }
    rooms[roomCode] = {
      users: [],
      maxUsers: parseInt(maxUsers, 10) || 2,
      endTime: Date.now() + duration,
    };
    console.log(`Room '${roomCode}' created for ${rooms[roomCode].maxUsers} users.`);
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

    // THE NEW, SAFER ARCHITECTURE: Tag the socket with its room.
    socket.roomCode = roomCode;
    room.users.push(socket.id);
    socket.join(roomCode);

    console.log(`User ${socket.id} joined room '${roomCode}'. Users in room: ${room.users.length}`);
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
    console.log(`User ${socket.id} disconnected.`);
    handleUserLeave(socket);
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, "0.0.0.0", () => {
  console.log(`ANONX Server is running on port ${PORT}`);
});



