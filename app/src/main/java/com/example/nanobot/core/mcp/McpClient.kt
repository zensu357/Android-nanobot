package com.example.nanobot.core.mcp

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI

interface McpClient {
    suspend fun discoverTools(server: McpServerDefinition): List<McpToolDescriptor>
    suspend fun callTool(server: McpServerDefinition, remoteToolName: String, arguments: JsonObject): String
}

@Singleton
class HttpMcpClient @Inject constructor() : McpClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun discoverTools(server: McpServerDefinition): List<McpToolDescriptor> = withContext(Dispatchers.IO) {
        validateEndpoint(server)
        val response = postJson(server.endpoint, buildRequestPayload("tools/list", buildJsonObject {}))
        val result = response["result"]?.jsonObject
            ?: throw IllegalStateException("MCP tools/list response was missing result.")
        val tools = result["tools"]?.jsonArray.orEmpty()

        tools.map { element ->
            parseToolDescriptor(server, element.jsonObject)
        }
    }

    override suspend fun callTool(server: McpServerDefinition, remoteToolName: String, arguments: JsonObject): String = withContext(Dispatchers.IO) {
        validateEndpoint(server)
        val response = postJson(
            server.endpoint,
            buildRequestPayload(
                method = "tools/call",
                params = buildJsonObject {
                    put("name", remoteToolName)
                    put("arguments", arguments)
                }
            )
        )
        val result = response["result"]?.jsonObject
            ?: throw IllegalStateException("MCP tools/call response was missing result.")
        val content = result["content"]?.jsonArray.orEmpty()
        if (content.isEmpty()) {
            return@withContext "MCP tool '$remoteToolName' completed without content."
        }

        content.joinToString("\n") { item ->
            val obj = item.jsonObject
            val text = obj["text"]?.jsonPrimitive?.contentOrNull
            val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
            text?.takeIf { it.isNotBlank() } ?: obj.toString().let {
                if (type.isBlank()) it else "$type: $it"
            }
        }
    }

    private fun parseToolDescriptor(server: McpServerDefinition, tool: JsonObject): McpToolDescriptor {
        val remoteName = tool["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        require(remoteName.isNotBlank()) { "MCP tool name cannot be blank." }
        val safeName = sanitizeName(remoteName)
        require(safeName.isNotBlank()) { "MCP tool name '$remoteName' is not supported." }
        val schemaElement = tool["inputSchema"]
        val inputSchema = when (schemaElement) {
            null -> buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
                put("required", buildJsonArray {})
            }
            is JsonObject -> schemaElement
            else -> throw IllegalArgumentException("MCP tool '$remoteName' returned a non-object input schema.")
        }
        require(isSupportedSchema(inputSchema)) { "MCP tool '$remoteName' returned an unsupported input schema." }
        return McpToolDescriptor(
            serverId = server.id,
            serverLabel = server.label,
            name = safeName,
            remoteName = remoteName,
            description = tool["description"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                .ifBlank { "MCP tool exposed by ${server.label}" },
            inputSchema = inputSchema,
            readOnlyHint = parseReadOnlyHint(tool["annotations"] ?: tool["meta"])
        )
    }

    private fun parseReadOnlyHint(element: JsonElement?): Boolean? {
        val obj = element as? JsonObject ?: return null
        return obj["readOnlyHint"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
    }

    private fun isSupportedSchema(schema: JsonObject): Boolean {
        val type = schema["type"]?.jsonPrimitive?.contentOrNull
        return type == null || type == "object"
    }

    private fun sanitizeName(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
    }

    private fun postJson(endpoint: String, payload: JsonObject): JsonObject {
        val request = Request.Builder()
            .url(endpoint)
            .post(json.encodeToString(JsonObject.serializer(), payload).toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("MCP request failed with HTTP ${response.code}.")
            }
            val body = response.body?.string().orEmpty()
            return json.parseToJsonElement(body).jsonObject
        }
    }

    private fun buildRequestPayload(method: String, params: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", JsonPrimitive(1))
        put("method", method)
        put("params", params)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    private fun validateEndpoint(server: McpServerDefinition) {
        val raw = server.endpoint.trim()
        require(raw.isNotBlank()) { "${server.label}: endpoint is required." }
        val uri = runCatching { URI(raw) }
            .getOrElse { throw IllegalArgumentException("${server.label}: endpoint is not a valid URL.") }
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") {
            "${server.label}: only http/https MCP endpoints are supported right now."
        }
        require(!uri.host.isNullOrBlank()) {
            "${server.label}: endpoint must include a host."
        }
    }
}
