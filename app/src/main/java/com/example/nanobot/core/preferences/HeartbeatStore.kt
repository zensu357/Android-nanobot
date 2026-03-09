package com.example.nanobot.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class HeartbeatStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("nanobot_heartbeat.preferences_pb") }
    )

    val heartbeatInstructions: Flow<String> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences -> preferences[HEARTBEAT_INSTRUCTIONS].orEmpty() }

    val heartbeatEnabled: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences -> preferences[HEARTBEAT_ENABLED] ?: DEFAULT_ENABLED }

    suspend fun setHeartbeatInstructions(value: String) {
        dataStore.edit { preferences ->
            preferences[HEARTBEAT_INSTRUCTIONS] = value.trim()
        }
    }

    suspend fun setHeartbeatEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[HEARTBEAT_ENABLED] = value
        }
    }

    private companion object {
        const val DEFAULT_ENABLED = true

        val HEARTBEAT_INSTRUCTIONS = stringPreferencesKey("heartbeat_instructions")
        val HEARTBEAT_ENABLED = booleanPreferencesKey("heartbeat_enabled")
    }
}
