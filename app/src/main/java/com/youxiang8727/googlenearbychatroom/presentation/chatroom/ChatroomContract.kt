package com.youxiang8727.googlenearbychatroom.presentation.chatroom

import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.model.Chatroom
import com.youxiang8727.googlenearbychatroom.presentation.base.UiEffect
import com.youxiang8727.googlenearbychatroom.presentation.base.UiEvent
import com.youxiang8727.googlenearbychatroom.presentation.base.UiState

class ChatroomContract {
    data class State(
        val messages: List<ChatMessage> = emptyList(),
        val discoveredChatrooms: List<Chatroom> = emptyList(),
        val isLoading: Boolean = false,
        val isAdvertising: Boolean = false,
        val isDiscovering: Boolean = false,
        val connectedEndpoints: List<String> = emptyList(),
        val userName: String = "",
        val userId: String = "",
        val chatroomName: String = "",
        val chatroomId: String = "",
        val isEditingName: Boolean = false
    ) : UiState

    sealed class Event : UiEvent {
        object StartDiscovery : Event()
        object StopDiscovery : Event()
        data class StartAdvertising(val chatroomName: String) : Event()
        object StopAdvertising : Event()
        data class ConnectToChatroom(val chatroom: Chatroom) : Event()
        data class SendMessage(val message: String) : Event()
        data class SendMedia(val uri: String, val type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType) : Event()
        object Disconnect : Event()
        data class SetUserName(val name: String) : Event()
        data class OnEditingNameChange(val isEditing: Boolean) : Event()
        object OnCreateRoomClick : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        object NavigateToChat : Effect()
        object NavigateToCreateRoom : Effect()
        object NavigateBack : Effect()
    }
}
