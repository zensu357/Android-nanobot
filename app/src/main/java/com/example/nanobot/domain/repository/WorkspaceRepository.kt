package com.example.nanobot.domain.repository

import com.example.nanobot.core.workspace.WorkspaceEntry
import com.example.nanobot.core.workspace.WorkspaceFileContent
import com.example.nanobot.core.workspace.WorkspaceReplaceResult
import com.example.nanobot.core.workspace.WorkspaceRoot
import com.example.nanobot.core.workspace.WorkspaceSearchHit
import com.example.nanobot.core.workspace.WorkspaceWriteResult

interface WorkspaceRepository {
    suspend fun getWorkspaceRoot(): WorkspaceRoot
    suspend fun list(relativePath: String = "", limit: Int = 50): List<WorkspaceEntry>
    suspend fun readText(relativePath: String, maxChars: Int = 4_000): WorkspaceFileContent
    suspend fun search(query: String, relativePath: String = "", limit: Int = 10): List<WorkspaceSearchHit>
    suspend fun writeText(relativePath: String, content: String, overwrite: Boolean = false): WorkspaceWriteResult
    suspend fun replaceText(
        relativePath: String,
        find: String,
        replaceWith: String,
        expectedOccurrences: Int? = null
    ): WorkspaceReplaceResult
}
