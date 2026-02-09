package com.example.anonx

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class JoinConversationActivity : AppCompatActivity() {

    private lateinit var btnJoin: Button
    private lateinit var etCode: EditText
    private lateinit var socket: Socket

    private val onJoinSuccess = Emitter.Listener {
        runOnUiThread {
            val roomCode = etCode.text.toString().trim()
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("ROOM_CODE", roomCode)
            startActivity(intent)
        }
    }

    private val onJoinError = Emitter.Listener { args ->
        runOnUiThread {
            val msg = args.firstOrNull()?.toString() ?: "Join failed"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            btnJoin.isEnabled = true
            btnJoin.text = "Join Room"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_join)

        etCode = findViewById(R.id.etCode)
        btnJoin = findViewById(R.id.btnJoin)

        socket = SocketManager.socket
        socket.on("joined", onJoinSuccess)
        socket.on("error-msg", onJoinError)

        btnJoin.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnJoin.isEnabled = false
            btnJoin.text = "Joining..."
            socket.emit("join-room", JSONObject().put("roomCode", code))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.off("joined", onJoinSuccess)
        socket.off("error-msg", onJoinError)
    }
}
