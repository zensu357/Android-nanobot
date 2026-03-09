package com.example.nanobot.core.model

enum class ProviderType(val wireValue: String) {
    OPENAI_COMPATIBLE("openai_compatible"),
    OPEN_ROUTER("openrouter"),
    AZURE_OPENAI("azure_openai");

    companion object {
        fun from(value: String?): ProviderType {
            return entries.firstOrNull { it.wireValue == value } ?: OPENAI_COMPATIBLE
        }
    }
}
