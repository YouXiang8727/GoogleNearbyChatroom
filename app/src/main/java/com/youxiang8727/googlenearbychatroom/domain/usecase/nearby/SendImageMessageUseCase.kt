package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import javax.inject.Inject

class SendImageMessageUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    suspend operator fun invoke(uri: String, type: com.youxiang8727.googlenearbychatroom.domain.model.MessageType, userName: String, userId: String) {
        repository.sendMediaMessage(uri, type, userName, userId)
    }
}
