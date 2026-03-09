package com.example.nanobot.core.web

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.preferences.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WebAccessConfigProviderImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : WebAccessConfigProvider {
    override suspend fun getConfig(): AgentConfig = settingsDataStore.configFlow.first()
}
