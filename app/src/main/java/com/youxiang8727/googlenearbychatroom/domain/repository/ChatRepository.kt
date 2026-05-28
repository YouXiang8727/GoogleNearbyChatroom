package com.youxiang8727.googlenearbychatroom.domain.repository

import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(chatroomId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun updateMessageStatus(messageId: String, status: com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus)
    suspend fun deleteMessagesByChatroom(chatroomId: String)
    suspend fun getExpiredVideos(expiryTime: Long): List<ChatMessage>
}
