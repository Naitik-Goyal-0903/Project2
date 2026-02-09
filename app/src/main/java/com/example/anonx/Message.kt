package com.example.anonx

/**
 * Represents a message in the chat. Now with support for different message types!
 * @param type The type of the message, can be "text" or "image".
 * @param content For "text", this is the message. For "image", this is the Base64 encoded image string.
 * @param sender Who sent the message. Can be "me", "other", or "system".
 */
data class Message(
    val type: String,
    val content: String,
    val sender: String
)
