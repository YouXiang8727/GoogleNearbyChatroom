package com.youxiang8727.googlenearbychatroom.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.model.Chatroom
import com.youxiang8727.googlenearbychatroom.domain.model.MessageType
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import com.youxiang8727.googlenearbychatroom.util.NotificationHelper
import com.youxiang8727.googlenearbychatroom.ChatApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
) : NearbyRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.youxiang8727.googlenearbychatroom.SERVICE_ID"
    private val gson = Gson()
    private val notificationHelper = NotificationHelper(context)

    private val _discoveredChatrooms = MutableStateFlow<List<Chatroom>>(emptyList())
    override val discoveredChatrooms = _discoveredChatrooms.asStateFlow()

    private val _messages = MutableSharedFlow<ChatMessage>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val messages = _messages.asSharedFlow()

    private val _isAdvertising = MutableStateFlow(false)
    override val isAdvertising = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering = _isDiscovering.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<List<String>>(emptyList())
    override val connectedEndpoints = _connectedEndpoints.asStateFlow()

    private var currentChatroomId: String = ""
    private val endpointNames = mutableMapOf<String, String>()
    private var isDisconnectingManually = false

    // To track incoming files. Map Payload ID to its Metadata message.
    private val incomingFileMetadata = mutableMapOf<Long, ChatMessage>()
    private val incomingFileSenderId = mutableMapOf<Long, String>()
    // To track file payloads that arrive before their metadata BYTES.
    private val completedFilePayloads = mutableMapOf<Long, Payload>()
    // To keep track of active FILE payloads received.
    private val activePayloads = mutableMapOf<Long, Payload>()
    private val activePayloadsSenderId = mutableMapOf<Long, String>()

    // To track outgoing payloads for delivery status. Map Payload ID to Message ID.
    private val outgoingPayloads = mutableMapOf<Long, String>()
    // To track how many payloads are still in flight for a specific message
    private val pendingPayloadsCount = mutableMapOf<String, Int>()

    private fun getFormattedName(endpointId: String): String {
        val rawName = endpointNames[endpointId] ?: return endpointId
        val parts = rawName.split("|")
        return when (parts.size) {
            3 -> "${parts[1]} #${parts[2]}" // chatroomName|userName|userId (from Host)
            2 -> "${parts[0]} #${parts[1]}" // userName|userId (from Guest)
            else -> rawName
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            endpointNames[endpointId] = info.endpointName
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                _connectedEndpoints.update { it + endpointId }
                // Only Host emits "joined" message to notify everyone
                // Guest shouldn't emit "Host joined" because Host is the room owner
                if (_isAdvertising.value) {
                    emitSystemMessage("${getFormattedName(endpointId)} joined the chatroom")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.update { it - endpointId }
            if (!isDisconnectingManually) {
                emitSystemMessage("${getFormattedName(endpointId)} left the chatroom")
            }
            endpointNames.remove(endpointId)
        }
    }

    private fun emitSystemMessage(content: String) {
        val systemMessage = ChatMessage(
            chatroomId = currentChatroomId,
            content = content,
            senderName = "System",
            isFromMe = false,
            isSystemMessage = true
        )
        repositoryScope.launch {
            _messages.emit(systemMessage)
        }
        if (_isAdvertising.value) {
            val json = gson.toJson(systemMessage)
            val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
            _connectedEndpoints.value.forEach { endpointId ->
                connectionsClient.sendPayload(endpointId, payload)
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.FILE) {
                activePayloads[payload.id] = payload
                activePayloadsSenderId[payload.id] = endpointId
            } else if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    try {
                        val json = String(bytes, Charsets.UTF_8)
                        val message = gson.fromJson(json, ChatMessage::class.java)
                        val receivedMessage = message.copy(isFromMe = false)

                        // If this is a metadata for a file, it uses filePayloadId for mapping
                        if (receivedMessage.filePayloadId != null) {
                            val completedPayload = completedFilePayloads.remove(receivedMessage.filePayloadId)
                            if (completedPayload != null) {
                                processFilePayload(receivedMessage, completedPayload, endpointId)
                            } else {
                                incomingFileMetadata[receivedMessage.filePayloadId] = receivedMessage
                                incomingFileSenderId[receivedMessage.filePayloadId] = endpointId
                            }
                        } else {
                            _messages.tryEmit(receivedMessage)
                            
                            // Background Notification Logic
                            val app = context.applicationContext as? ChatApplication
                            if (app?.isAppInBackground == true) {
                                notificationHelper.showNotification(receivedMessage)
                            }

                            if (_isAdvertising.value) {
                                _connectedEndpoints.value.forEach { otherEndpointId ->
                                    if (otherEndpointId != endpointId) {
                                        connectionsClient.sendPayload(otherEndpointId, Payload.fromBytes(bytes))
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                // Check if this was an outgoing payload
                outgoingPayloads.remove(update.payloadId)?.let { messageId ->
                    val remaining = (pendingPayloadsCount[messageId] ?: 1) - 1
                    if (remaining <= 0) {
                        pendingPayloadsCount.remove(messageId)
                        repositoryScope.launch {
                            chatRepository.updateMessageStatus(messageId, com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENT)
                        }
                    } else {
                        pendingPayloadsCount[messageId] = remaining
                    }
                }

                val payload = activePayloads.remove(update.payloadId)
                val senderId = activePayloadsSenderId.remove(update.payloadId) ?: endpointId
                if (payload != null && (payload.type == Payload.Type.FILE)) {
                    val metadata = incomingFileMetadata.remove(payload.id)
                    val metadataSenderId = incomingFileSenderId.remove(payload.id) ?: senderId
                    if (metadata != null) {
                        processFilePayload(metadata, payload, metadataSenderId)
                    } else {
                        completedFilePayloads[payload.id] = payload
                        incomingFileSenderId[payload.id] = metadataSenderId
                    }
                }
            } else if (update.status == PayloadTransferUpdate.Status.FAILURE || update.status == PayloadTransferUpdate.Status.CANCELED) {
                outgoingPayloads.remove(update.payloadId)?.let { messageId ->
                    pendingPayloadsCount.remove(messageId)
                    repositoryScope.launch {
                        chatRepository.updateMessageStatus(messageId, com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.FAILED)
                    }
                }
            }
        }
    }

    private fun processFilePayload(metadata: ChatMessage, payload: Payload, senderEndpointId: String) {
        val filePayload = payload.asFile() ?: return
        val extension = when (metadata.type) {
            MessageType.IMAGE -> "jpg"
            MessageType.VIDEO -> "mp4"
            MessageType.GIF -> "gif"
            else -> "dat"
        }
        
        val roomDir = File(context.filesDir, "media_${metadata.chatroomId}")
        if (!roomDir.exists()) roomDir.mkdirs()
        
        val permanentFile = File(roomDir, "media_${System.currentTimeMillis()}.$extension")

        // Thumbnail logic
        var thumbnailUri: String? = null
        if (metadata.type == MessageType.VIDEO) {
            try {
                val retriever = MediaMetadataRetriever()
                val uri = filePayload.asUri()
                if (uri != null) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.setDataSource(filePayload.asJavaFile()?.absolutePath)
                }
                val bitmap = retriever.getFrameAtTime(0)
                if (bitmap != null) {
                    val thumbFile = File(roomDir, "thumb_${System.currentTimeMillis()}.jpg")
                    thumbFile.outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                    }
                    thumbnailUri = Uri.fromFile(thumbFile).toString()
                }
                retriever.release()
            } catch (e: Exception) {
                Log.e("NearbyRepository", "Failed to generate thumbnail", e)
            }
        }

        try {
            val uri = filePayload.asUri()
            if (uri != null) {
                // Prefer using URI and ContentResolver for better compatibility with Scoped Storage
                context.contentResolver.openInputStream(uri)?.use { input ->
                    permanentFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                val file = filePayload.asJavaFile() ?: return
                // Try to rename, if fails (e.g. cross-partition), copy and delete
                if (!file.renameTo(permanentFile)) {
                    file.copyTo(permanentFile, overwrite = true)
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("NearbyRepository", "Failed to move file to permanent location", e)
            return
        }

        val finalMessage = metadata.copy(
            mediaUri = Uri.fromFile(permanentFile).toString(),
            thumbnailUri = thumbnailUri
        )
        _messages.tryEmit(finalMessage)

        // Background Notification for Files
        val app = context.applicationContext as? ChatApplication
        if (app?.isAppInBackground == true) {
            notificationHelper.showNotification(finalMessage)
        }

        if (_isAdvertising.value) {
            repositoryScope.launch {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(Uri.fromFile(permanentFile), "r") ?: return@launch
                    val relayFilePayload = Payload.fromFile(pfd)
                    val relayMetadata = finalMessage.copy(isFromMe = false, filePayloadId = relayFilePayload.id)
                    val relayMetadataBytes = gson.toJson(relayMetadata).toByteArray(Charsets.UTF_8)

                    _connectedEndpoints.value.forEach { id ->
                        if (id != senderEndpointId) {
                            connectionsClient.sendPayload(id, relayFilePayload)
                            connectionsClient.sendPayload(id, Payload.fromBytes(relayMetadataBytes))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NearbyRepository", "Failed to relay file payload", e)
                }
            }
        }
    }

    override suspend fun startAdvertising(userName: String, userId: String, chatroomName: String) {
        isDisconnectingManually = false
        currentChatroomId = userId // Use Host's Unique ID as Chatroom ID
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        try {
            connectionsClient.startAdvertising(
                "$chatroomName|$userName|$userId",
                serviceId,
                connectionLifecycleCallback,
                options
            ).await()
            _isAdvertising.value = true
        } catch (e: Exception) {
            _isAdvertising.value = false
            throw e
        }
    }

    override suspend fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
    }

    override suspend fun startDiscovery() {
        isDisconnectingManually = false
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        _discoveredChatrooms.value = emptyList()
        try {
            connectionsClient.startDiscovery(
                serviceId,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        val parts = info.endpointName.split("|")
                        val chatroomName = parts.getOrNull(0) ?: info.endpointName
                        val hostId = parts.getOrNull(2) ?: ""
                        
                        _discoveredChatrooms.update { currentList ->
                            currentList.filterNot { it.id == endpointId } + Chatroom(
                                id = endpointId,
                                name = chatroomName,
                                hostId = hostId
                            )
                        }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        _discoveredChatrooms.update { list -> list.filterNot { it.id == endpointId } }
                    }
                },
                options
            ).await()
            _isDiscovering.value = true
        } catch (e: Exception) {
            _isDiscovering.value = false
            throw e
        }
    }

    override suspend fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        _discoveredChatrooms.value = emptyList()
    }

    override suspend fun connectTo(chatroom: Chatroom, userName: String, userId: String) {
        isDisconnectingManually = false
        currentChatroomId = chatroom.hostId // Use the room owner's ID
        try {
            connectionsClient.requestConnection("$userName|$userId", chatroom.id, connectionLifecycleCallback).await()
        } catch (_: Exception) {}
    }

    override suspend fun sendMessage(message: String, userName: String, userId: String) {
        val chatMessage = ChatMessage(
            chatroomId = currentChatroomId,
            content = message,
            senderName = userName,
            senderId = userId,
            isFromMe = true,
            status = com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENDING
        )
        val json = gson.toJson(chatMessage)
        val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
        
        // Save locally first
        chatRepository.saveMessage(chatMessage)
        outgoingPayloads[payload.id] = chatMessage.id
        pendingPayloadsCount[chatMessage.id] = _connectedEndpoints.value.size

        _connectedEndpoints.value.forEach { connectionsClient.sendPayload(it, payload) }
    }

    override suspend fun sendMediaMessage(
        uri: String,
        type: MessageType,
        userName: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val contentUri = uri.toUri()

            val roomDir = File(context.filesDir, "media_$currentChatroomId")
            if (!roomDir.exists()) roomDir.mkdirs()

            // Copy file to internal storage for persistence
            val extension = when (type) {
                MessageType.IMAGE -> "jpg"
                MessageType.VIDEO -> "mp4"
                MessageType.GIF -> "gif"
                else -> "dat"
            }
            val localFile = File(roomDir, "sent_media_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val localMediaUri = Uri.fromFile(localFile).toString()

            // Thumbnail for Host
            var thumbnailUri: String? = null
            if (type == MessageType.VIDEO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, contentUri)
                    val bitmap = retriever.getFrameAtTime(0)
                    if (bitmap != null) {
                        val thumbFile = File(roomDir, "thumb_${System.currentTimeMillis()}.jpg")
                        thumbFile.outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                        }
                        thumbnailUri = Uri.fromFile(thumbFile).toString()
                    }
                    retriever.release()
                } catch (e: Exception) {
                    Log.e("NearbyRepository", "Failed to generate thumbnail for outgoing video", e)
                }
            }

            val pfd = context.contentResolver.openFileDescriptor(localMediaUri.toUri(), "r") ?: return@withContext
            val filePayload = Payload.fromFile(pfd)

            val chatMessage = ChatMessage(
                chatroomId = currentChatroomId,
                content = "Shared a ${type.name.lowercase()}",
                senderName = userName,
                senderId = userId,
                isFromMe = true,
                type = type,
                mediaUri = localMediaUri,
                thumbnailUri = thumbnailUri,
                filePayloadId = filePayload.id,
                status = com.youxiang8727.googlenearbychatroom.domain.model.MessageStatus.SENDING
            )

            val json = gson.toJson(chatMessage)
            val metadataPayload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))

            // Save locally first
            chatRepository.saveMessage(chatMessage)
            // For media, we have 2 payloads per endpoint (File + Metadata)
            val endpointCount = _connectedEndpoints.value.size
            outgoingPayloads[filePayload.id] = chatMessage.id
            outgoingPayloads[metadataPayload.id] = chatMessage.id
            pendingPayloadsCount[chatMessage.id] = endpointCount * 2

            _connectedEndpoints.value.forEach { endpointId ->
                connectionsClient.sendPayload(endpointId, filePayload)
                connectionsClient.sendPayload(endpointId, metadataPayload)
            }
        } catch (e: Exception) {
            Log.e("NearbyRepository", "Failed to send media message", e)
            throw e
        }
    }

    override suspend fun clearChatroomMedia(chatroomId: String) {
        withContext(Dispatchers.IO) {
            val roomDir = File(context.filesDir, "media_$chatroomId")
            if (roomDir.exists()) {
                roomDir.deleteRecursively()
            }
        }
    }

    override suspend fun downloadMedia(uri: String, type: MessageType) {
        withContext(Dispatchers.IO) {
            try {
                val sourceUri = Uri.parse(uri)
                val fileName = "Nearby_${System.currentTimeMillis()}.${if (type == MessageType.VIDEO) "mp4" else if (type == MessageType.GIF) "gif" else "jpg"}"
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, when (type) {
                        MessageType.IMAGE -> "image/jpeg"
                        MessageType.VIDEO -> "video/mp4"
                        MessageType.GIF -> "image/gif"
                        else -> "application/octet-stream"
                    })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, if (type == MessageType.VIDEO) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val collection = if (type == MessageType.VIDEO) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val destinationUri = context.contentResolver.insert(collection, contentValues) ?: return@withContext

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        input.copyTo(output)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(destinationUri, contentValues, null, null)
                }
            } catch (e: Exception) {
                Log.e("NearbyRepository", "Failed to download media", e)
                throw e
            }
        }
    }

    override suspend fun disconnect() {
        isDisconnectingManually = true
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptyList()
        stopAdvertising()
        stopDiscovery()
    }
}
