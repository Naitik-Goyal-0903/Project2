package com.example.anonx

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class MakeConversationActivity : AppCompatActivity() {

    private lateinit var btnCreate: Button
    private lateinit var btnJoinNow: Button
    private lateinit var tvCode: TextView
    private lateinit var btnCopy: Button
    private lateinit var socket: Socket

    private var createdRoomCode: String? = null

    private val onConnect = Emitter.Listener {
        runOnUiThread {
            btnCreate.isEnabled = true
            btnCreate.text = "Generate Code"
        }
    }

    private val onDisconnect = Emitter.Listener {
        runOnUiThread {
            btnCreate.isEnabled = false
            btnCreate.text = "Connecting..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_make)

        val etTime = findViewById<EditText>(R.id.etTime)
        val etUsers = findViewById<EditText>(R.id.etUsers)
        btnCreate = findViewById(R.id.btnCreate)
        btnJoinNow = findViewById(R.id.btnJoinNow)
        tvCode = findViewById(R.id.tvCode)
        btnCopy = findViewById(R.id.btnCopy)

        initSocket()

        btnCreate.setOnClickListener {

            if (createdRoomCode != null) return@setOnClickListener

            val time = etTime.text.toString()
            val users = etUsers.text.toString()

            if (time.isEmpty() || users.isEmpty()) {
                Toast.makeText(this, "Enter time and users", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val roomCode = CodeGenerator.generateCode()
            createdRoomCode = roomCode

            val durationInMs = time.toLong() * 60 * 1000

            val roomData = JSONObject().apply {
                put("roomCode", roomCode)
                put("duration", durationInMs)
                put("maxUsers", users.toInt())
            }

            socket.emit("create-room", roomData)

            // ✅ HOST AUTO JOIN
            socket.emit("join-room", JSONObject().put("roomCode", roomCode))

            displayRoomCode(roomCode)
        }

        btnJoinNow.setOnClickListener {
            createdRoomCode?.let {
                val intent = Intent(this, ChatRoomActivity::class.java)
                intent.putExtra("ROOM_CODE", it)
                startActivity(intent)
            }
        }

        btnCopy.setOnClickListener {
            val codeText = tvCode.text.toString().replace("Room Code: ", "")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("RoomCode", codeText))
            Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initSocket() {
        socket = SocketManager.socket
        socket.on(Socket.EVENT_CONNECT, onConnect)
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect)

        if (!socket.connected()) {
            SocketManager.establishConnection()
        }
    }

    private fun displayRoomCode(code: String) {
        tvCode.text = "Room Code: $code"
        btnCopy.visibility = View.VISIBLE
        btnJoinNow.visibility = View.VISIBLE
        btnCreate.text = "Room Created"
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off(Socket.EVENT_DISCONNECT, onDisconnect)
    }
}
