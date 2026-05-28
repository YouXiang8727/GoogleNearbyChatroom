package com.youxiang8727.googlenearbychatroom.domain.usecase

import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatroomId: String): Flow<List<ChatMessage>> {
        return repository.getMessages(chatroomId)
    }
}
