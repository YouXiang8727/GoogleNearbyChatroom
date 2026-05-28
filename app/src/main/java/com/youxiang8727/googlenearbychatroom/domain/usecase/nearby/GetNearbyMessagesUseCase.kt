package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.model.ChatMessage
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNearbyMessagesUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    operator fun invoke(): Flow<ChatMessage> {
        return repository.messages
    }
}
