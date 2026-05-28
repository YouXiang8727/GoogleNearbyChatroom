package com.youxiang8727.googlenearbychatroom.data.repository

import com.youxiang8727.googlenearbychatroom.data.local.MessageDao
import com.youxiang8727.googlenearbychatroom.data.local.entity.MessageEntity
import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus
import com.youxiang8727.googlenearbychatroom.domain.model.MessageType
import com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : ChatRepository {
    override fun getMessages(chatroomId: String): Flow<List<ChatMessage>> = messageDao.getMessagesByChatroom(chatroomId).map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                chatroomId = entity.chatroomId,
                content = entity.content,
                senderName = entity.senderName,
                senderId = entity.senderId,
                isFromMe = entity.isFromMe,
                isSystemMessage = entity.isSystemMessage,
                type = MessageType.valueOf(entity.type),
                mediaUri = entity.mediaUri,
                thumbnailUri = entity.thumbnailUri,
                status = MessageStatus.valueOf(entity.status),
                timestamp = entity.timestamp
            )
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(
            MessageEntity(
                id = message.id,
                chatroomId = message.chatroomId,
                content = message.content,
                senderName = message.senderName,
                senderId = message.senderId,
                isFromMe = message.isFromMe,
                isSystemMessage = message.isSystemMessage,
                type = message.type.name,
                mediaUri = message.mediaUri,
                thumbnailUri = message.thumbnailUri,
                status = message.status.name,
                timestamp = message.timestamp
            )
        )
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status.name)
    }

    override suspend fun getExpiredVideos(expiryTime: Long): List<ChatMessage> {
        return messageDao.getExpiredVideos(expiryTime).map { entity ->
            ChatMessage(
                id = entity.id,
                content = entity.content,
                senderName = entity.senderName,
                senderId = entity.senderId,
                isFromMe = entity.isFromMe,
                isSystemMessage = entity.isSystemMessage,
                type = MessageType.valueOf(entity.type),
                mediaUri = entity.mediaUri,
                thumbnailUri = entity.thumbnailUri,
                status = MessageStatus.valueOf(entity.status),
                timestamp = entity.timestamp
            )
        }
    }
}
