package com.example.anonx

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketManager {

    private const val SERVER_URL = "https://project2-20c8.onrender.com"

    val socket: Socket by lazy {
        try {
            val opts = IO.Options()

            // 🔒 Render-safe transport
            opts.transports = arrayOf("polling")

            // 🔥 Reconnection control (IMPORTANT)
            opts.reconnection = true
            opts.reconnectionAttempts = Int.MAX_VALUE
            opts.reconnectionDelay = 2000        // 2 sec
            opts.reconnectionDelayMax = 5000     // max 5 sec
            opts.timeout = 20000                 // 20 sec

            val socket = IO.socket(SERVER_URL, opts)

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Socket Connected")
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                // ❌ spam log hata diya
                Log.d("SocketManager", "Socket Disconnected")
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()
                Log.e("SocketManager", "Connect error: $error")
            }

            socket
        } catch (e: URISyntaxException) {
            Log.e("SocketManager", "Invalid Server URL: $SERVER_URL")
            throw RuntimeException(e)
        }
    }

    fun establishConnection() {
        if (!socket.connected()) {
            socket.connect()
        }
    }

    // ⚠️ DO NOT auto-call this from lifecycle
    fun closeConnection() {
        if (socket.connected()) {
            socket.disconnect()
        }
    }
}
