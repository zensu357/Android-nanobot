package com.example.nanobot.core.workspace

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WorkspaceFileService private constructor(
    private val context: Context?,
    private val workspaceRootOverride: File?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    internal constructor(workspaceRootOverride: File) : this(null, workspaceRootOverride)

    suspend fun getWorkspaceRoot(): WorkspaceRoot = withContext(Dispatchers.IO) {
        WorkspaceRoot(isAvailable = ensureRootExists())
    }

    suspend fun list(relativePath: String, limit: Int): List<WorkspaceEntry> = withContext(Dispatchers.IO) {
        val target = resolveRelativePath(relativePath)
        if (!target.exists()) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' does not exist.")
        }
        if (!target.isDirectory) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' is not a directory.")
        }

        target.listFiles()
            .orEmpty()
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            .take(limit.coerceIn(1, MAX_LIST_LIMIT))
            .map { file ->
                WorkspaceEntry(
                    relativePath = toRelativePath(file),
                    isDirectory = file.isDirectory,
                    sizeBytes = file.takeIf { it.isFile }?.length()
                )
            }
    }

    suspend fun readText(relativePath: String, maxChars: Int): WorkspaceFileContent = withContext(Dispatchers.IO) {
        val target = resolveRelativePath(relativePath)
        if (!target.exists()) {
            throw IllegalArgumentException("Workspace file '${displayPath(relativePath)}' does not exist.")
        }
        if (!target.isFile) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' is not a file.")
        }

        val byteLimit = maxChars.coerceIn(1, MAX_READ_CHARS) * MAX_BYTES_PER_CHAR
        val bytes = readUpTo(target, byteLimit + 1)
        if (looksBinary(bytes)) {
            throw IllegalArgumentException("Workspace file '${toRelativePath(target)}' appears to be binary and cannot be read as text.")
        }

        val decoded = bytes.toString(Charsets.UTF_8)
        val truncated = bytes.size > byteLimit || decoded.length > maxChars.coerceIn(1, MAX_READ_CHARS)
        val safeContent = decoded.take(maxChars.coerceIn(1, MAX_READ_CHARS))

        WorkspaceFileContent(
            relativePath = toRelativePath(target),
            content = safeContent,
            truncated = truncated,
            totalBytes = target.length()
        )
    }

    suspend fun search(query: String, relativePath: String, limit: Int): List<WorkspaceSearchHit> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            throw IllegalArgumentException("Search query cannot be blank.")
        }

        val target = resolveRelativePath(relativePath)
        if (!target.exists()) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' does not exist.")
        }

        val hitLimit = limit.coerceIn(1, MAX_SEARCH_HITS)
        val files = if (target.isFile) sequenceOf(target) else target.walkTopDown().filter { it.isFile }
        val hits = mutableListOf<WorkspaceSearchHit>()

        for (file in files) {
            if (hits.size >= hitLimit) break
            if (file.length() > MAX_SEARCH_FILE_BYTES) continue

            val sampleBytes = readUpTo(file, BINARY_SAMPLE_BYTES)
            if (looksBinary(sampleBytes)) continue

            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineNumber = 0
                for (line in lines) {
                    lineNumber += 1
                    if (!line.contains(normalizedQuery, ignoreCase = true)) continue

                    hits += WorkspaceSearchHit(
                        relativePath = toRelativePath(file),
                        lineNumber = lineNumber,
                        snippet = line.trim().take(MAX_SNIPPET_CHARS)
                    )
                    if (hits.size >= hitLimit) {
                        break
                    }
                }
            }
        }

        hits
    }

    suspend fun writeText(relativePath: String, content: String, overwrite: Boolean): WorkspaceWriteResult = withContext(Dispatchers.IO) {
        val target = resolveRelativePath(relativePath)
        val normalizedContent = content
        val encoded = normalizedContent.toByteArray(StandardCharsets.UTF_8)
        if (encoded.size > MAX_WRITE_BYTES) {
            throw IllegalArgumentException("Workspace write exceeds the maximum allowed size of ${MAX_WRITE_BYTES} bytes.")
        }

        val parent = target.parentFile ?: throw IllegalArgumentException("Workspace file must be inside the sandbox.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("Failed to create parent directories for '${displayPath(relativePath)}'.")
        }
        if (target.exists() && target.isDirectory) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' is a directory and cannot be overwritten as a file.")
        }
        if (target.exists() && !overwrite) {
            throw IllegalArgumentException("Workspace file '${displayPath(relativePath)}' already exists. Set overwrite=true to replace it.")
        }
        if (target.exists()) {
            validateTextFile(target)
        }

        val created = !target.exists()
        writeAtomically(target, encoded)
        WorkspaceWriteResult(
            relativePath = toRelativePath(target),
            created = created,
            bytesWritten = encoded.size.toLong()
        )
    }

    suspend fun replaceText(
        relativePath: String,
        find: String,
        replaceWith: String,
        expectedOccurrences: Int?
    ): WorkspaceReplaceResult = withContext(Dispatchers.IO) {
        val target = resolveRelativePath(relativePath)
        if (!target.exists()) {
            throw IllegalArgumentException("Workspace file '${displayPath(relativePath)}' does not exist.")
        }
        if (!target.isFile) {
            throw IllegalArgumentException("Workspace path '${displayPath(relativePath)}' is not a file.")
        }
        if (find.isEmpty()) {
            throw IllegalArgumentException("The replacement target cannot be empty.")
        }

        validateTextFile(target)
        val original = target.readText(StandardCharsets.UTF_8)
        val occurrences = countOccurrences(original, find)
        if (occurrences == 0) {
            throw IllegalArgumentException("The target text was not found in '${displayPath(relativePath)}'.")
        }
        if (expectedOccurrences != null && occurrences != expectedOccurrences) {
            throw IllegalArgumentException("Expected $expectedOccurrences occurrence(s) but found $occurrences in '${displayPath(relativePath)}'.")
        }

        val updated = original.replace(find, replaceWith)
        val encoded = updated.toByteArray(StandardCharsets.UTF_8)
        if (encoded.size > MAX_WRITE_BYTES) {
            throw IllegalArgumentException("Workspace write exceeds the maximum allowed size of ${MAX_WRITE_BYTES} bytes.")
        }

        writeAtomically(target, encoded)
        WorkspaceReplaceResult(
            relativePath = toRelativePath(target),
            replacements = occurrences,
            bytesWritten = encoded.size.toLong()
        )
    }

    private fun ensureRootExists(): Boolean {
        val root = workspaceRoot()
        return root.exists() || root.mkdirs()
    }

    private fun resolveRelativePath(relativePath: String): File {
        check(ensureRootExists()) { "Workspace sandbox is unavailable." }

        val root = workspaceRoot().canonicalFile
        val normalized = normalizeRelativePath(relativePath)
        val candidate = if (normalized.isEmpty()) root else File(root, normalized).canonicalFile

        if (candidate.path != root.path && !candidate.path.startsWith(root.path + File.separator)) {
            throw SecurityException("Workspace access is limited to workspace:/")
        }
        return candidate
    }

    private fun normalizeRelativePath(relativePath: String): String {
        val trimmed = relativePath.trim().replace('\\', '/')
        if (trimmed.isBlank() || trimmed == ".") {
            return ""
        }
        if (trimmed.startsWith("/") || WINDOWS_DRIVE_REGEX.containsMatchIn(trimmed)) {
            throw SecurityException("Absolute paths are not allowed in workspace tools.")
        }

        val segments = trimmed.split('/')
            .filter { it.isNotBlank() && it != "." }
        if (segments.any { it == ".." }) {
            throw SecurityException("Parent path traversal is not allowed in workspace tools.")
        }

        return segments.joinToString(File.separator)
    }

    private fun toRelativePath(file: File): String {
        val root = workspaceRoot().canonicalFile
        val target = file.canonicalFile
        val relative = target.relativeTo(root).invariantSeparatorsPath
        return if (relative.isBlank()) "." else relative
    }

    private fun displayPath(relativePath: String): String {
        val normalized = relativePath.trim().replace('\\', '/').ifBlank { "." }
        return if (normalized == ".") "workspace:/" else "workspace:/$normalized"
    }

    private fun workspaceRoot(): File {
        return workspaceRootOverride ?: File(
            checkNotNull(context) { "WorkspaceFileService requires a context or explicit workspace root." }.filesDir,
            WORKSPACE_DIRECTORY_NAME
        )
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tempFile = File(target.parentFile, target.name + ".tmp")
        try {
            tempFile.writeBytes(bytes)
            if (target.exists() && !target.delete()) {
                throw IllegalStateException("Failed to replace existing workspace file '${target.name}'.")
            }
            if (!tempFile.renameTo(target)) {
                throw IllegalStateException("Failed to finalize workspace write for '${target.name}'.")
            }
        } finally {
            if (tempFile.exists() && tempFile != target) {
                tempFile.delete()
            }
        }
    }

    private fun readUpTo(file: File, maxBytes: Int): ByteArray {
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val output = ByteArrayOutputStream()
            while (output.size() < maxBytes) {
                val remaining = maxBytes - output.size()
                val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }

    private fun looksBinary(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        if (bytes.any { it == 0.toByte() }) return true

        val suspicious = bytes.count { byte ->
            val value = byte.toInt() and 0xFF
            value < 0x09 || value in 0x0E..0x1F || value == 0x7F
        }
        return suspicious * 5 > bytes.size
    }

    private fun validateTextFile(file: File) {
        if (file.length() > MAX_WRITE_BYTES) {
            throw IllegalArgumentException("Workspace file '${toRelativePath(file)}' is too large to edit safely.")
        }
        val bytes = readUpTo(file, BINARY_SAMPLE_BYTES)
        if (looksBinary(bytes)) {
            throw IllegalArgumentException("Workspace file '${toRelativePath(file)}' appears to be binary and cannot be edited as text.")
        }
        runCatching {
            file.readText(StandardCharsets.UTF_8)
        }.getOrElse { throwable ->
            if (throwable is MalformedInputException) {
                throw IllegalArgumentException("Workspace file '${toRelativePath(file)}' is not valid UTF-8 text.")
            }
            throw throwable
        }
    }

    private fun countOccurrences(text: String, target: String): Int {
        var index = 0
        var count = 0
        while (true) {
            val found = text.indexOf(target, startIndex = index)
            if (found == -1) return count
            count += 1
            index = found + target.length
        }
    }

    private companion object {
        const val WORKSPACE_DIRECTORY_NAME = "workspace"
        const val MAX_LIST_LIMIT = 100
        const val MAX_READ_CHARS = 6_000
        const val MAX_BYTES_PER_CHAR = 4
        const val MAX_WRITE_BYTES = 128 * 1024
        const val MAX_SEARCH_HITS = 20
        const val MAX_SEARCH_FILE_BYTES = 256 * 1024L
        const val MAX_SNIPPET_CHARS = 180
        const val BINARY_SAMPLE_BYTES = 1024
        val WINDOWS_DRIVE_REGEX = Regex("^[A-Za-z]:")
    }
}
