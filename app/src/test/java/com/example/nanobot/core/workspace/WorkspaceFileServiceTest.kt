package com.example.nanobot.core.workspace

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class WorkspaceFileServiceTest {
    private val workspaceRoot = Files.createTempDirectory("nanobot-workspace-test").toFile()

    @AfterTest
    fun tearDown() {
        workspaceRoot.deleteRecursively()
    }

    @Test
    fun writeTextCreatesNewFile() = runTest {
        val service = WorkspaceFileService(workspaceRoot)

        val result = service.writeText("src/hello.txt", "hello", overwrite = false)

        assertTrue(result.created)
        assertEquals("hello", File(workspaceRoot, "src/hello.txt").readText())
    }

    @Test
    fun writeTextOverwritesExistingFileWhenAllowed() = runTest {
        val service = WorkspaceFileService(workspaceRoot)
        service.writeText("notes.txt", "old", overwrite = false)

        val result = service.writeText("notes.txt", "new", overwrite = true)

        assertTrue(!result.created)
        assertEquals("new", File(workspaceRoot, "notes.txt").readText())
    }

    @Test
    fun replaceTextUpdatesExpectedOccurrenceCount() = runTest {
        val service = WorkspaceFileService(workspaceRoot)
        service.writeText("notes.txt", "alpha beta alpha", overwrite = false)

        val result = service.replaceText("notes.txt", "alpha", "gamma", expectedOccurrences = 2)

        assertEquals(2, result.replacements)
        assertEquals("gamma beta gamma", File(workspaceRoot, "notes.txt").readText())
    }

    @Test
    fun writeTextRejectsTraversal() = runTest {
        val service = WorkspaceFileService(workspaceRoot)

        assertFailsWith<SecurityException> {
            service.writeText("../escape.txt", "bad", overwrite = false)
        }
    }

    @Test
    fun replaceTextRejectsBinaryFile() = runTest {
        val service = WorkspaceFileService(workspaceRoot)
        val binaryFile = File(workspaceRoot, "image.bin")
        binaryFile.parentFile?.mkdirs()
        binaryFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        val error = assertFailsWith<IllegalArgumentException> {
            service.replaceText("image.bin", "a", "b", expectedOccurrences = 1)
        }
        assertTrue(error.message.orEmpty().contains("binary", ignoreCase = true))
    }
}
