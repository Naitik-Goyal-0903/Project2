package com.example.anonx

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // --- View Types ---
    private companion object {
        private const val VIEW_TYPE_SENT_TEXT = 1
        private const val VIEW_TYPE_RECEIVED_TEXT = 2
        private const val VIEW_TYPE_SYSTEM = 3
        private const val VIEW_TYPE_SENT_IMAGE = 4
        private const val VIEW_TYPE_RECEIVED_IMAGE = 5
    }

    // --- ViewHolders (The safe and standard way) ---

    /** A ViewHolder for standard text messages (both sent and received). */
    private inner class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
    }

    /** A ViewHolder for image messages. */
    private inner class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageImageView: ImageView = itemView.findViewById(R.id.messageImageView)
    }
    
    /** A ViewHolder for system messages (e.g., "User has joined"). */
    private inner class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val systemMessageTextView: TextView = itemView.findViewById(R.id.systemMessageTextView)
    }


    // --- Core Adapter Methods ---

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when (message.sender) {
            "me" -> if (message.type == "image") VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_SENT_TEXT
            "system" -> VIEW_TYPE_SYSTEM
            else -> if (message.type == "image") VIEW_TYPE_RECEIVED_IMAGE else VIEW_TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Inflate the correct layout and create the correct ViewHolder for the message type.
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_text_sent, parent, false)
                TextMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_text_received, parent, false)
                TextMessageViewHolder(view)
            }
            VIEW_TYPE_SENT_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_image_sent, parent, false)
                ImageMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_image_received, parent, false)
                ImageMessageViewHolder(view)
            }
            else -> { // VIEW_TYPE_SYSTEM
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_system, parent, false)
                SystemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        // Bind the data to the correct ViewHolder, which prevents any casting errors.
        when (holder) {
            is TextMessageViewHolder -> {
                holder.messageTextView.text = message.content
            }
            is ImageMessageViewHolder -> {
                // THE FINAL, CRASH-PROOF FIX for images:
                try {
                    val imageBytes = Base64.decode(message.content, Base64.DEFAULT)
                    val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.messageImageView.setImageBitmap(imageBitmap)
                } catch (e: Exception) {
                    // If decoding fails, log the error and show a placeholder or nothing.
                    // This prevents the app from crashing under any circumstance.
                    Log.e("ChatAdapter", "Error decoding image: ${e.message}")
                    holder.messageImageView.setImageResource(android.R.drawable.ic_menu_report_image) // Error icon
                }
            }
            is SystemViewHolder -> {
                holder.systemMessageTextView.text = message.content
            }
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        // Notify the adapter that a new item has been added at the very end of the list.
        notifyItemInserted(messages.size - 1)
    }
}
