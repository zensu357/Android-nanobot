package com.example.nanobot.core.tools

import javax.inject.Inject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ToolValidator @Inject constructor() {
    fun validate(arguments: JsonObject, schema: JsonObject): ValidationResult {
        if (arguments.keys.any { it.isBlank() }) {
            return ValidationResult(false, "Argument keys cannot be blank.")
        }

        val requiredFields = (schema["required"] as? JsonArray).orEmpty().mapNotNull {
            it.jsonPrimitive.contentOrNull
        }
        val missing = requiredFields.filter { key ->
            val value = arguments[key] ?: return@filter true
            val primitiveContent = value.toString().trim('"')
            primitiveContent.isBlank() || value.toString() == "null"
        }

        return if (missing.isEmpty()) {
            ValidationResult(true, null)
        } else {
            ValidationResult(false, "Missing required fields: ${missing.joinToString()}")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)
