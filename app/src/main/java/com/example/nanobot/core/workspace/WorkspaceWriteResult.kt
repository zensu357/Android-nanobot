package com.example.nanobot.core.workspace

data class WorkspaceWriteResult(
    val relativePath: String,
    val created: Boolean,
    val bytesWritten: Long
)

data class WorkspaceReplaceResult(
    val relativePath: String,
    val replacements: Int,
    val bytesWritten: Long
)
