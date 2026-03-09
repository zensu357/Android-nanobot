package com.example.nanobot.core.preferences

import androidx.test.core.app.ApplicationProvider
import com.example.nanobot.core.mcp.McpServerDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class McpServerStoreTest {
    @Test
    fun savesAndReadsMcpServers() = runTest {
        val store = McpServerStore(ApplicationProvider.getApplicationContext())
        val servers = listOf(
            McpServerDefinition(
                id = "github",
                label = "GitHub MCP",
                endpoint = "https://mcp.example/github",
                enabled = true
            )
        )

        store.saveServers(servers)

        assertEquals(servers, store.getServersSnapshot())
    }
}
