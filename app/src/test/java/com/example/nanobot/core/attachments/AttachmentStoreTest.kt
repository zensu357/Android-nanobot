package com.example.nanobot.core.attachments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val attachmentStore = AttachmentStore(context)

    @AfterTest
    fun tearDown() {
        File(context.filesDir, "attachments").deleteRecursively()
    }

    @Test
    fun buildDataUrlRejectsMissingFile() = runTest {
        val error = assertFailsWith<IllegalArgumentException> {
            attachmentStore.buildDataUrl("attachments/images/missing.jpg", "image/jpeg")
        }

        assertTrue(error.message.orEmpty().contains("missing"))
    }

    @Test
    fun buildDataUrlPreparesOversizedImageIntoSafeDataUrl() = runTest {
        val file = File(context.filesDir, "attachments/images/huge.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(2_000_000) { 1 })
        }

        val dataUrl = attachmentStore.buildDataUrl(
            file.relativeTo(context.filesDir).path.replace('\\', '/'),
            "image/jpeg"
        )

        assertTrue(dataUrl.startsWith("data:image/jpeg;base64,"))
    }
}
