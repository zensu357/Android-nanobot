package com.example.nanobot.core.workspace

data class WorkspaceRoot(
    val rootId: String = "workspace:/",
    val isAvailable: Boolean,
    val accessMode: String = "read_only"
)

data class WorkspaceEntry(
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null
)

data class WorkspaceFileContent(
    val relativePath: String,
    val content: String,
    val truncated: Boolean,
    val totalBytes: Long
)

data class WorkspaceSearchHit(
    val relativePath: String,
    val lineNumber: Int,
    val snippet: String
)
