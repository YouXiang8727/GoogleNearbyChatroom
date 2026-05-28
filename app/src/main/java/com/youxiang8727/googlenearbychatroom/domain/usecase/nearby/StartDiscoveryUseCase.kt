package com.youxiang8727.googlenearbychatroom.domain.usecase.nearby

import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import javax.inject.Inject

class StartDiscoveryUseCase @Inject constructor(
    private val repository: NearbyRepository
) {
    suspend operator fun invoke() {
        repository.startDiscovery()
    }
}
