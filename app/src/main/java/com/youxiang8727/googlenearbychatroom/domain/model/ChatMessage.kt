package com.youxiang8727.googlenearbychatroom.domain.model

enum class MessageType {
    TEXT, IMAGE, VIDEO, GIF, KICK
}

enum class MessageStatus {
    SENDING, SENT, FAILED
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatroomId: String = "",
    val content: String,
    val senderName: String,
    val senderId: String = "",
    val isFromMe: Boolean,
    val isSystemMessage: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val thumbnailUri: String? = null,
    val filePayloadId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)
