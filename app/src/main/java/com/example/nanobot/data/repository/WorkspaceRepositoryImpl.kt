package com.example.nanobot.data.repository

import com.example.nanobot.core.workspace.WorkspaceEntry
import com.example.nanobot.core.workspace.WorkspaceFileContent
import com.example.nanobot.core.workspace.WorkspaceFileService
import com.example.nanobot.core.workspace.WorkspaceReplaceResult
import com.example.nanobot.core.workspace.WorkspaceRoot
import com.example.nanobot.core.workspace.WorkspaceSearchHit
import com.example.nanobot.core.workspace.WorkspaceWriteResult
import com.example.nanobot.domain.repository.WorkspaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val workspaceFileService: WorkspaceFileService
) : WorkspaceRepository {
    override suspend fun getWorkspaceRoot(): WorkspaceRoot = workspaceFileService.getWorkspaceRoot()

    override suspend fun list(relativePath: String, limit: Int): List<WorkspaceEntry> {
        return workspaceFileService.list(relativePath, limit)
    }

    override suspend fun readText(relativePath: String, maxChars: Int): WorkspaceFileContent {
        return workspaceFileService.readText(relativePath, maxChars)
    }

    override suspend fun search(query: String, relativePath: String, limit: Int): List<WorkspaceSearchHit> {
        return workspaceFileService.search(query, relativePath, limit)
    }

    override suspend fun writeText(relativePath: String, content: String, overwrite: Boolean): WorkspaceWriteResult {
        return workspaceFileService.writeText(relativePath, content, overwrite)
    }

    override suspend fun replaceText(
        relativePath: String,
        find: String,
        replaceWith: String,
        expectedOccurrences: Int?
    ): WorkspaceReplaceResult {
        return workspaceFileService.replaceText(relativePath, find, replaceWith, expectedOccurrences)
    }
}
