package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.model.Chatroom
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import javax.inject.Inject

class ConnectToChatroomUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    suspend operator fun invoke(chatroom: Chatroom, userName: String, userId: String) {
        repository.connectTo(chatroom, userName, userId)
    }
}
