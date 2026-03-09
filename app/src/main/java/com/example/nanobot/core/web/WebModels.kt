package com.example.nanobot.core.web

data class WebFetchResult(
    val url: String,
    val title: String? = null,
    val contentType: String? = null,
    val content: String,
    val truncated: Boolean
)

data class WebSearchItem(
    val title: String,
    val url: String,
    val snippet: String
)

data class WebSearchResult(
    val query: String,
    val results: List<WebSearchItem>
)
