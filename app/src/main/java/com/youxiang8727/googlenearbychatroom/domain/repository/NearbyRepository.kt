package com.youxiang8727.googlenearbychatroom.domain.repository

import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.model.Chatroom
import kotlinx.coroutines.flow.Flow

interface NearbyRepository {
    val discoveredChatrooms: Flow<List<Chatroom>>
    val messages: Flow<ChatMessage>
    val isAdvertising: Flow<Boolean>
    val isDiscovering: Flow<Boolean>
    val connectedEndpoints: Flow<List<String>>

    suspend fun startAdvertising(userName: String, userId: String, chatroomName: String)
    suspend fun stopAdvertising()
    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun connectTo(chatroom: Chatroom, userName: String, userId: String)
    suspend fun sendMessage(message: String, userName: String, userId: String)
    suspend fun sendMediaMessage(uri: String, type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType, userName: String, userId: String)
    suspend fun clearChatroomMedia(chatroomId: String)
    suspend fun downloadMedia(uri: String, type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType)
    suspend fun disconnect()

    companion object {
        const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
        const val MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
    }
}
