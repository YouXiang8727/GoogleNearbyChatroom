package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import javax.inject.Inject

class StartAdvertisingUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    suspend operator fun invoke(userName: String, userId: String, chatroomName: String) {
        repository.startAdvertising(userName, userId, chatroomName)
    }
}
