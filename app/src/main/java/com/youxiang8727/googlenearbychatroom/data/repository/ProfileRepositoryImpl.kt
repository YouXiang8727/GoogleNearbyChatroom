package com.youxiang8727.googlenearbychatroom.data.repository

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.youxiang8727.googlenearbychatroom.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : ProfileRepository {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
    }

    override fun getUniqueId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    override suspend fun getUserName(): String {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.USER_NAME] ?: android.os.Build.MODEL
    }

    override suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }
}
