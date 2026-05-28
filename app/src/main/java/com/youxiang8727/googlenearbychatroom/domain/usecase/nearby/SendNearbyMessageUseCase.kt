package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import javax.inject.Inject

class SendNearbyMessageUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    suspend operator fun invoke(message: String, userName: String, userId: String) {
        repository.sendMessage(message, userName, userId)
    }
}
