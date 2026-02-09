package com.example.anonx

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ChatRoomActivity : AppCompatActivity() {

    private lateinit var socket: Socket
    private lateinit var timerText: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var roomCode: String
    private var timer: CountDownTimer? = null
    private val messages = mutableListOf<Message>()
    private val mySenderId = "me"

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val base64 = contentResolver.openInputStream(it)?.use { s -> encodeImage(s) }
                if (base64 != null) {
                    val encrypted = EncryptionHelper.encrypt(base64, roomCode) ?: return@let
                    chatAdapter.addMessage(Message("image", base64, mySenderId))

                    val data = JSONObject().apply {
                        put("roomCode", roomCode)
                        put("message", JSONObject().apply {
                            put("type", "image")
                            put("content", encrypted)
                        })
                    }
                    socket.emit("send-message", data)
                }
            }
        }

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            val data = args[0] as? JSONObject ?: return@runOnUiThread
            val decrypted =
                EncryptionHelper.decrypt(data.optString("content"), roomCode) ?: return@runOnUiThread

            chatAdapter.addMessage(
                Message(data.optString("type"), decrypted, "other")
            )
            chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private val onSystemMessage = Emitter.Listener { args ->
        runOnUiThread {
            chatAdapter.addMessage(
                Message("system", args.firstOrNull()?.toString() ?: "", "system")
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_chat)

        roomCode = intent.getStringExtra("ROOM_CODE") ?: run { finish(); return }

        timerText = findViewById(R.id.timerText)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendBtn = findViewById<Button>(R.id.sendBtn)
        val leaveBtn = findViewById<Button>(R.id.leaveBtn)
        val attachBtn = findViewById<ImageButton>(R.id.attachBtn)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        socket = SocketManager.socket

        sendBtn.setOnClickListener {
            val msg = messageInput.text.toString().trim()
            if (msg.isEmpty()) return@setOnClickListener

            val encrypted = EncryptionHelper.encrypt(msg, roomCode) ?: return@setOnClickListener
            chatAdapter.addMessage(Message("text", msg, mySenderId))

            val data = JSONObject().apply {
                put("roomCode", roomCode)
                put("message", JSONObject().apply {
                    put("type", "text")
                    put("content", encrypted)
                })
            }
            socket.emit("send-message", data)
            messageInput.setText("")
        }

        attachBtn.setOnClickListener { selectImageLauncher.launch("image/*") }

        leaveBtn.setOnClickListener {
            socket.emit("leave-room")
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        // 🔥 REMOVE THEN ADD (NO DUPLICATES)
        socket.off("new-message", onNewMessage)
        socket.off("system", onSystemMessage)

        socket.on("new-message", onNewMessage)
        socket.on("system", onSystemMessage)

        if (!socket.connected()) socket.connect()
    }

    override fun onStop() {
        super.onStop()
        socket.off("new-message", onNewMessage)
        socket.off("system", onSystemMessage)
        timer?.cancel()
    }

    private fun encodeImage(input: InputStream): String {
        val bmp = BitmapFactory.decodeStream(input)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, out)
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }
}
