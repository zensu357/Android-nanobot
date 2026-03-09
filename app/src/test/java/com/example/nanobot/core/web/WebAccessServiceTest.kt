package com.example.nanobot.core.web

import com.example.nanobot.core.model.AgentConfig
import java.net.InetAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class WebAccessServiceTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchReturnsReadableTextFromPublicUrl() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody(
                    """
                    <html>
                      <head><title>Example Title</title></head>
                      <body>
                        <h1>Hello Android Nanobot</h1>
                        <script>window.secret = 'ignore';</script>
                        <p>This page contains useful visible text for fetch testing.</p>
                      </body>
                    </html>
                    """.trimIndent()
                )
        )

        val service = createService()
        val result = service.fetch(publicUrl("/page"), 120)

        assertEquals("Example Title", result.title)
        assertTrue(result.content.contains("Hello Android Nanobot"))
        assertFalse(result.content.contains("window.secret"))
    }

    @Test
    fun fetchRejectsRedirectToLocalhost() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "http://localhost:${server.port}/private")
        )

        val service = createService()

        val error = assertFailsWith<IllegalArgumentException> {
            service.fetch(publicUrl("/redirect"), 500)
        }
        assertTrue(error.message.orEmpty().contains("Localhost"))
    }

    @Test
    fun fetchRejectsUnsupportedContentType() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "image/png")
                .setBody("not really an image")
        )

        val service = createService()

        val error = assertFailsWith<IllegalArgumentException> {
            service.fetch(publicUrl("/image"), 500)
        }
        assertTrue(error.message.orEmpty().contains("Unsupported web content type"))
    }

    @Test
    fun searchRequiresConfiguredApiKey() = runTest {
        val service = createService(config = AgentConfig(webSearchApiKey = ""))

        val error = assertFailsWith<IllegalStateException> {
            service.search("android", 5)
        }
        assertTrue(error.message.orEmpty().contains("Web Search API Key"))
    }

    @Test
    fun searchParsesResultsFromConfiguredEndpoint() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "organic": [
                        {
                          "title": "WorkManager guide",
                          "link": "https://developer.android.com/topic/libraries/architecture/workmanager",
                          "snippet": "Use WorkManager for persistent background work on Android."
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val service = createService(
            config = AgentConfig(webSearchApiKey = "test-key"),
            searchEndpoint = publicUrl("/search")
        )
        val result = service.search("android workmanager", 5)

        assertEquals("android workmanager", result.query)
        assertEquals(1, result.results.size)
        assertTrue(result.results.first().title.contains("WorkManager"))
    }

    @Test
    fun fetchRejectsInvalidProxyConfiguration() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/plain")
                .setBody("hello")
        )

        val service = createService(config = AgentConfig(webProxy = "not-a-valid-proxy"))

        val error = assertFailsWith<IllegalArgumentException> {
            service.fetch(publicUrl("/proxy"), 200)
        }
        assertTrue(error.message.orEmpty().contains("proxy", ignoreCase = true))
    }

    private fun createService(
        config: AgentConfig = AgentConfig(),
        searchEndpoint: String = publicUrl("/search")
    ): WebAccessService {
        val testHost = TEST_HOST.lowercase()
        val requestGuard = WebRequestGuard(
            delegateDns = Dns.SYSTEM,
            hostOverrides = mapOf(testHost to listOf(InetAddress.getByName("127.0.0.1"))),
            allowPrivateHostsForTesting = setOf(testHost)
        )
        return WebAccessService(
            configProvider = FakeConfigProvider(config),
            searchEndpointProvider = FakeSearchEndpointProvider(searchEndpoint),
            safeDns = SafeDns(requestGuard),
            webRequestGuard = requestGuard
        )
    }

    private fun publicUrl(path: String): String {
        return server.url(path)
            .newBuilder()
            .host(TEST_HOST)
            .build()
            .toString()
    }

    private class FakeConfigProvider(
        private val config: AgentConfig
    ) : WebAccessConfigProvider {
        override suspend fun getConfig(): AgentConfig = config
    }

    private class FakeSearchEndpointProvider(
        private val endpoint: String
    ) : WebSearchEndpointProvider {
        override fun getSearchEndpoint(): String = endpoint
    }

    private companion object {
        const val TEST_HOST = "public.test"
    }
}
