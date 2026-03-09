package com.example.nanobot.domain.repository

import com.example.nanobot.core.web.WebFetchResult
import com.example.nanobot.core.web.WebSearchResult

interface WebAccessRepository {
    suspend fun fetch(url: String, maxChars: Int = 4_000): WebFetchResult
    suspend fun search(query: String, limit: Int = 5): WebSearchResult
}
