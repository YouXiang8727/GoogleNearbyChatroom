package com.youxiang8727.googlenearbychatroom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatroomId: String = "",
    val content: String,
    val senderName: String,
    val senderId: String = "",
    val isFromMe: Boolean,
    val isSystemMessage: Boolean = false,
    val type: String = "TEXT",
    val mediaUri: String? = null,
    val thumbnailUri: String? = null,
    val status: String = "SENT",
    val timestamp: Long
)
