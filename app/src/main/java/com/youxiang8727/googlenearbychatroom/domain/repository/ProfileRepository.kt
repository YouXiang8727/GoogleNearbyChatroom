package com.youxiang8727.googlenearbychatroom.domain.repository

interface ProfileRepository {
    fun getUniqueId(): String
    suspend fun getUserName(): String
    suspend fun setUserName(name: String)
}
