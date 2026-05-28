package com.youxiang8727.googlenearbychatroom.presentation.chatroom

import androidx.lifecycle.viewModelScope
import com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import com.youxiang8727.googlenearbychatroom.domain.repository.ProfileRepository
import com.youxiang8727.googlenearbychatroom.domain.usecase.nearby.*
import com.youxiang8727.googlenearbychatroom.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatroomViewModel @Inject constructor(
    private val startAdvertisingUseCase: StartAdvertisingUseCase,
    private val startDiscoveryUseCase: StartDiscoveryUseCase,
    private val connectToChatroomUseCase: ConnectToChatroomUseCase,
    private val getDiscoveredChatroomsUseCase: GetDiscoveredChatroomsUseCase,
    private val getNearbyMessagesUseCase: GetNearbyMessagesUseCase,
    private val sendNearbyMessageUseCase: SendNearbyMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val getMessagesUseCase: com.youxiang8727.googlenearbychatroom.domain.usecase.GetMessagesUseCase,
    private val chatRepository: ChatRepository,
    private val nearbyRepository: NearbyRepository,
    private val profileRepository: ProfileRepository,
) : BaseViewModel<ChatroomContract.State, ChatroomContract.Event, ChatroomContract.Effect>() {

    init {
        loadUserInfo()
        observeNearbyStates()
        observeLocalMessages()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val name = profileRepository.getUserName()
            val id = profileRepository.getUniqueId().takeLast(4)
            setState { 
                copy(
                    userName = name, 
                    userId = id,
                    chatroomName = "$name's Chatroom"
                ) 
            }
        }
    }

    override fun createInitialState(): ChatroomContract.State {
        return ChatroomContract.State()
    }

    override fun handleEvent(event: ChatroomContract.Event) {
        when (event) {
            is ChatroomContract.Event.StartDiscovery -> startDiscovery()
            is ChatroomContract.Event.StopDiscovery -> stopDiscovery()
            is ChatroomContract.Event.StartAdvertising -> startAdvertising(event.chatroomName)
            is ChatroomContract.Event.StopAdvertising -> stopAdvertising()
            is ChatroomContract.Event.ConnectToChatroom -> connectToChatroom(event.chatroom)
            is ChatroomContract.Event.SendMessage -> sendMessage(event.message)
            is ChatroomContract.Event.SendMedia -> sendMedia(event.uri, event.type)
            is ChatroomContract.Event.DeleteChatHistory -> deleteHistory(event.chatroomId)
            is ChatroomContract.Event.DownloadMedia -> downloadMedia(event.uri, event.type)
            is ChatroomContract.Event.Disconnect -> disconnect()
            is ChatroomContract.Event.SetUserName -> updateUserName(event.name)
            is ChatroomContract.Event.OnEditingNameChange -> {
                setState { copy(isEditingName = event.isEditing) }
            }
            is ChatroomContract.Event.OnCreateRoomClick -> {
                setEffect { ChatroomContract.Effect.NavigateToCreateRoom }
            }
        }
    }

    private fun updateUserName(name: String) {
        viewModelScope.launch {
            profileRepository.setUserName(name)
            setState { 
                copy(
                    userName = name, 
                    isEditingName = false,
                    chatroomName = "$name's Chatroom"
                ) 
            }
        }
    }

    private fun observeLocalMessages() {
        viewModelScope.launch {
            // Observe changes in chatroomId or isAdvertising to switch room subscription
            uiState.map { state -> 
                if (state.isAdvertising) state.userId else state.chatroomId 
            }.distinctUntilChanged()
             .collectLatest { roomToObserve ->
                if (roomToObserve.isNotEmpty()) {
                    getMessagesUseCase(roomToObserve).collect { messages ->
                        setState { copy(messages = messages) }
                    }
                }
            }
        }
    }

    private fun observeNearbyStates() {
        viewModelScope.launch {
            getDiscoveredChatroomsUseCase().collect { chatrooms ->
                setState { copy(discoveredChatrooms = chatrooms) }
            }
        }
        viewModelScope.launch {
            getNearbyMessagesUseCase().collect { message ->
                android.util.Log.d("ChatViewModel", "Received nearby message: ${message.content}")
                chatRepository.saveMessage(message)
            }
        }
        viewModelScope.launch {
            nearbyRepository.isAdvertising.collect { isAdvertising ->
                setState { copy(isAdvertising = isAdvertising) }
            }
        }
        viewModelScope.launch {
            nearbyRepository.isDiscovering.collect { isDiscovering ->
                setState { copy(isDiscovering = isDiscovering) }
            }
        }
        viewModelScope.launch {
            nearbyRepository.connectedEndpoints.collect { endpoints ->
                setState { copy(connectedEndpoints = endpoints) }
            }
        }
    }

    private fun startDiscovery() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                startDiscoveryUseCase()
            } catch (e: Exception) {
                setEffect { ChatroomContract.Effect.ShowToast("Discovery failed: ${e.message}") }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun stopDiscovery() {
        viewModelScope.launch {
            nearbyRepository.stopDiscovery()
            setState { copy(discoveredChatrooms = emptyList()) }
        }
    }

    private fun startAdvertising(chatroomName: String) {
        viewModelScope.launch {
            try {
                nearbyRepository.stopDiscovery() // Stop discovery when hosting
                setState { copy(chatroomName = chatroomName, isLoading = true) }
                startAdvertisingUseCase(uiState.value.userName, uiState.value.userId, chatroomName)
                setEffect { ChatroomContract.Effect.NavigateToChat }
            } catch (e: Exception) {
                setEffect { ChatroomContract.Effect.ShowToast("Failed to start hosting: ${e.message}") }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun stopAdvertising() {
        viewModelScope.launch {
            nearbyRepository.stopAdvertising()
            setState { copy(messages = emptyList(), chatroomId = "") }
        }
    }

    private fun connectToChatroom(chatroom: com.youxiang8727.googlenearbychatroom.domain.model.Chatroom) {
        viewModelScope.launch {
            setState { copy(chatroomId = chatroom.hostId) }
            connectToChatroomUseCase(chatroom, uiState.value.userName, uiState.value.userId)
        }
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            sendNearbyMessageUseCase(message, uiState.value.userName, uiState.value.userId)
        }
    }

    private fun sendMedia(uri: String, type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType) {
        viewModelScope.launch {
            try {
                sendImageMessageUseCase(uri, type, uiState.value.userName, uiState.value.userId)
            } catch (e: Exception) {
                setEffect { ChatroomContract.Effect.ShowToast(e.message ?: "Failed to send media") }
            }
        }
    }

    private fun deleteHistory(chatroomId: String) {
        viewModelScope.launch {
            // Delete DB records
            chatRepository.deleteMessagesByChatroom(chatroomId)
            
            // Delete Physical Files from Storage
            nearbyRepository.clearChatroomMedia(chatroomId)
        }
    }

    private fun downloadMedia(uri: String, type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType) {
        viewModelScope.launch {
            try {
                nearbyRepository.downloadMedia(uri, type)
                setEffect { ChatroomContract.Effect.ShowToast("Media saved to gallery") }
            } catch (e: Exception) {
                setEffect { ChatroomContract.Effect.ShowToast("Failed to save media: ${e.message}") }
            }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            nearbyRepository.disconnect()
            setState { copy(connectedEndpoints = emptyList(), messages = emptyList(), chatroomId = "") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            nearbyRepository.disconnect()
        }
    }
}
