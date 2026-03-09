package com.example.nanobot.core.ai

data class PromptSection(
    val title: String,
    val body: List<String>
) {
    fun isEmpty(): Boolean = body.none { it.isNotBlank() }

    fun render(): String {
        return buildString {
            appendLine("## $title")
            body.filter { it.isNotBlank() }.forEach { appendLine(it) }
        }.trim()
    }
}

data class SystemPromptContent(
    val sections: List<PromptSection>
) {
    fun render(): String = sections
        .filterNot { it.isEmpty() }
        .joinToString(separator = "\n\n") { it.render() }
}
