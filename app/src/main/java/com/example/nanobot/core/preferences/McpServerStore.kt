package com.example.nanobot.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.nanobot.core.mcp.McpServerDefinition
import com.example.nanobot.core.mcp.McpToolDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface McpServerConfigStore {
    fun observeServers(): Flow<List<McpServerDefinition>>
    fun observeCachedTools(): Flow<List<McpToolDescriptor>>
    fun getServersSnapshot(): List<McpServerDefinition>
    fun getCachedToolsSnapshot(): List<McpToolDescriptor>
    suspend fun saveServers(servers: List<McpServerDefinition>)
    suspend fun saveCachedTools(tools: List<McpToolDescriptor>)
}

@Singleton
class McpServerStore @Inject constructor(
    @ApplicationContext context: Context
) : McpServerConfigStore {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("nanobot_mcp.preferences_pb") }
    )

    @Volatile
    private var serversSnapshot: List<McpServerDefinition> = emptyList()

    @Volatile
    private var cachedToolsSnapshot: List<McpToolDescriptor> = emptyList()

    override fun observeServers(): Flow<List<McpServerDefinition>> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            parseServers(preferences[SERVERS_JSON]).also { serversSnapshot = it }
        }

    override fun observeCachedTools(): Flow<List<McpToolDescriptor>> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            parseTools(preferences[CACHED_TOOLS_JSON]).also { cachedToolsSnapshot = it }
        }

    override fun getServersSnapshot(): List<McpServerDefinition> = serversSnapshot.ifEmpty {
        runBlocking { observeServers().first() }
    }

    override fun getCachedToolsSnapshot(): List<McpToolDescriptor> = cachedToolsSnapshot.ifEmpty {
        runBlocking { observeCachedTools().first() }
    }

    override suspend fun saveServers(servers: List<McpServerDefinition>) {
        serversSnapshot = servers
        dataStore.edit { preferences ->
            preferences[SERVERS_JSON] = json.encodeToString(
                ListSerializer(McpServerDefinition.serializer()),
                servers
            )
        }
    }

    override suspend fun saveCachedTools(tools: List<McpToolDescriptor>) {
        cachedToolsSnapshot = tools
        dataStore.edit { preferences ->
            preferences[CACHED_TOOLS_JSON] = json.encodeToString(
                ListSerializer(McpToolDescriptor.serializer()),
                tools
            )
        }
    }

    private fun parseServers(raw: String?): List<McpServerDefinition> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(McpServerDefinition.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun parseTools(raw: String?): List<McpToolDescriptor> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(McpToolDescriptor.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        val SERVERS_JSON = stringPreferencesKey("mcp_servers_json")
        val CACHED_TOOLS_JSON = stringPreferencesKey("mcp_cached_tools_json")
    }
}
