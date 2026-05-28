package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.model.Chatroom
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiscoveredChatroomsUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    operator fun invoke(): Flow<List<Chatroom>> {
        return repository.discoveredChatrooms
    }
}
