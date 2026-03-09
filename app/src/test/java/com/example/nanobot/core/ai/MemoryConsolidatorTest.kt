package com.example.nanobot.core.ai

import com.example.nanobot.core.model.AgentConfig
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.LlmChatRequest
import com.example.nanobot.core.model.MemoryFact
import com.example.nanobot.core.model.MemorySummary
import com.example.nanobot.core.model.MessageRole
import com.example.nanobot.core.model.ProviderChatResult
import com.example.nanobot.domain.repository.ChatRepository
import com.example.nanobot.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryConsolidatorTest {
    @Test
    fun consolidatesSummaryAndFactCandidate() = runTest {
        val repository = FakeMemoryRepository()
        val consolidator = MemoryConsolidator(
            memoryRepository = repository,
            chatRepository = FakeChatRepository(),
            memoryPromptBuilder = MemoryPromptBuilder()
        )

        val result = consolidator.consolidate(
            sessionId = "session-1",
            history = listOf(
                ChatMessage(sessionId = "session-1", role = MessageRole.USER, content = "I am building an Android assistant."),
                ChatMessage(sessionId = "session-1", role = MessageRole.ASSISTANT, content = "Nice."),
                ChatMessage(sessionId = "session-1", role = MessageRole.USER, content = "Please remember that I prefer Kotlin."),
                ChatMessage(sessionId = "session-1", role = MessageRole.ASSISTANT, content = "Understood.")
            ),
            config = AgentConfig(enableMemory = true)
        )

        assertTrue(result)
        assertNotNull(repository.summary)
        assertTrue(repository.summary!!.summary.contains("Android assistant"))
        assertEquals("The user is building an Android assistant.", repository.facts.single().fact)
    }

    private class FakeMemoryRepository : MemoryRepository {
        val facts = mutableListOf<MemoryFact>()
        var summary: MemorySummary? = null

        override fun observeFacts(): Flow<List<MemoryFact>> = flowOf(facts)
        override fun observeSummaries(): Flow<List<MemorySummary>> = flowOf(listOfNotNull(summary))
        override suspend fun getFacts(): List<MemoryFact> = facts
        override suspend fun getAllSummaries(): List<MemorySummary> = listOfNotNull(summary)
        override suspend fun getFactsForQuery(query: String): List<MemoryFact> =
            facts.filter { it.fact.contains(query, ignoreCase = true) }
        override suspend fun observeSummariesSnapshot(): List<MemorySummary> = listOfNotNull(summary)
        override suspend fun getSummaryForSession(sessionId: String): MemorySummary? = summary
        override suspend fun upsertFact(fact: MemoryFact) {
            facts += fact
        }
        override suspend fun upsertSummary(summary: MemorySummary) {
            this.summary = summary
        }
    }

    private class FakeChatRepository : ChatRepository {
        override suspend fun completeChat(request: LlmChatRequest, config: AgentConfig): ProviderChatResult {
            return ProviderChatResult(
                content = """
                    {"updatedSummary":"The user is building an Android assistant and prefers Kotlin.","candidateFacts":["The user is building an Android assistant."]}
                """.trimIndent()
            )
        }
    }
}
