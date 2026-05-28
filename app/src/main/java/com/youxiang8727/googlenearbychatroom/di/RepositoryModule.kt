package com.youxiang8727.googlenearbychatroom.di

import com.youxiang8727.googlenearbychatroom.data.repository.ChatRepositoryImpl
import com.youxiang8727.googlenearbychatroom.data.repository.NearbyRepositoryImpl
import com.youxiang8727.googlenearbychatroom.data.repository.ProfileRepositoryImpl
import com.youxiang8727.googlenearbychatroom.domain.repository.ChatRepository
import com.youxiang8727.googlenearbychatroom.domain.repository.NearbyRepository
import com.youxiang8727.googlenearbychatroom.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindNearbyRepository(
        nearbyRepositoryImpl: NearbyRepositoryImpl
    ): NearbyRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository
}
