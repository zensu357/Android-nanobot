package com.example.nanobot.core.web

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWebSearchEndpointProvider @Inject constructor() : WebSearchEndpointProvider {
    override fun getSearchEndpoint(): String = DEFAULT_SEARCH_ENDPOINT

    private companion object {
        const val DEFAULT_SEARCH_ENDPOINT = "https://google.serper.dev/search"
    }
}
