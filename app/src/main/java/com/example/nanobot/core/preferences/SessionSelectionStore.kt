package com.example.nanobot.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
class SessionSelectionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("nanobot_session.preferences_pb") }
    )

    val selectedSessionId: Flow<String?> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences -> preferences[SELECTED_SESSION_ID] }

    suspend fun setSelectedSessionId(sessionId: String?) {
        dataStore.edit { preferences ->
            if (sessionId.isNullOrBlank()) {
                preferences.remove(SELECTED_SESSION_ID)
            } else {
                preferences[SELECTED_SESSION_ID] = sessionId
            }
        }
    }

    private companion object {
        val SELECTED_SESSION_ID = stringPreferencesKey("selected_session_id")
    }
}
