package com.example.nanobot.core.memory

import com.example.nanobot.core.model.MemoryFact

object MemoryFactGovernance {
    fun findReplacementCandidate(existingFacts: Collection<MemoryFact>, candidateFact: String): MemoryFact? {
        val normalizedCandidate = normalize(candidateFact)
        existingFacts.firstOrNull { normalize(it.fact) == normalizedCandidate }?.let { return it }

        val candidateKey = slotKey(candidateFact) ?: return null
        return existingFacts.firstOrNull { slotKey(it.fact) == candidateKey }
    }

    private fun slotKey(fact: String): String? {
        val normalized = normalize(fact)
        val prefixes = listOf(
            "the user prefers " to "prefers",
            "the user likes " to "likes",
            "the user dislikes " to "dislikes",
            "the user wants " to "wants",
            "the user needs " to "needs"
        )
        val (prefix, family) = prefixes.firstOrNull { (prefix) -> normalized.startsWith(prefix) } ?: return null
        val tail = normalized.removePrefix(prefix).trim()
        val bucket = detectBucket(tail) ?: return null
        return "$family:$bucket"
    }

    private fun detectBucket(tail: String): String? {
        return when {
            RESPONSE_STYLE_KEYWORDS.any { tail.contains(it) } -> "response_style"
            TECHNOLOGY_KEYWORDS.any { tail.contains(it) } -> "technology"
            PLATFORM_KEYWORDS.any { tail.contains(it) } -> "platform"
            TOOLING_KEYWORDS.any { tail.contains(it) } -> "tooling"
            else -> null
        }
    }

    private fun normalize(value: String): String = value.trim().lowercase()

    private val RESPONSE_STYLE_KEYWORDS = listOf(
        "answer", "answers", "reply", "replies", "response", "responses",
        "concise", "brief", "short", "detailed", "detail", "long"
    )

    private val TECHNOLOGY_KEYWORDS = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "rust",
        "go", "swift", "c#", "c++", "compose", "react", "flutter"
    )

    private val PLATFORM_KEYWORDS = listOf(
        "android", "ios", "web", "backend", "frontend", "desktop"
    )

    private val TOOLING_KEYWORDS = listOf(
        "gradle", "room", "retrofit", "okhttp", "hilt", "workmanager", "git"
    )
}
