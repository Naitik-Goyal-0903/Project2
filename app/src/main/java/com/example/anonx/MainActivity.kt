package com.example.anonx

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // THE FIX: This line blocks screenshots and screen recording
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_main)

        // Buttons
        val btnMake = findViewById<ImageView>(R.id.btnMake)
        val btnJoin = findViewById<ImageView>(R.id.btnJoin)

        // Make Conversation button click
        btnMake.setOnClickListener {
            //val intent = Intent(this, MakeConversationActivity::class.java)
            //startActivity(intent)
        }

        // Join Conversation button click
        btnJoin.setOnClickListener {
            //val intent = Intent(this, JoinConversationActivity::class.java)
            //startActivity(intent)
        }
    }
}