package com.example.nanobot.core.ai

import com.example.nanobot.core.model.Attachment
import com.example.nanobot.core.model.AttachmentType
import com.example.nanobot.core.model.ChatMessage
import com.example.nanobot.core.model.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmMessageMapperTest {
    @Test
    fun mapsImageAttachmentsIntoLlmAttachmentDtos() {
        val message = ChatMessage(
            sessionId = "session-1",
            role = MessageRole.USER,
            content = "Check this image",
            attachments = listOf(
                Attachment(
                    id = "attachment-1",
                    type = AttachmentType.IMAGE,
                    displayName = "photo.png",
                    mimeType = "image/png",
                    sizeBytes = 512L,
                    localPath = "attachments/images/photo.png"
                )
            )
        )

        val llmMessage = message.toLlmMessage()

        assertEquals(1, llmMessage.attachments.size)
        assertEquals("image", llmMessage.attachments.single().type)
        assertEquals("attachments/images/photo.png", llmMessage.attachments.single().localPath)
        assertTrue(llmMessage.content.toString().contains("Check this image"))
    }
}
