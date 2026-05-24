package com.virtualworld.easyexpensecontrol.core.util

/**
 * Elimina claves de API y parámetros sensibles de textos mostrados al usuario o en logs.
 */
object SensitiveDataSanitizer {

    private val GOOGLE_API_KEY_PATTERN = Regex("AIza[0-9A-Za-z_-]{20,}")
    private val URL_KEY_PARAM_PATTERN = Regex("([?&]key=)[^&\\s\"']+", RegexOption.IGNORE_CASE)

    fun sanitize(message: String?, knownSecrets: Collection<String> = emptyList()): String {
        val text = message?.takeIf { it.isNotBlank() } ?: return ""

        var sanitized = text
        knownSecrets
            .filter { it.isNotBlank() }
            .forEach { secret -> sanitized = sanitized.replace(secret, "[REDACTED]") }

        return sanitized
            .replace(URL_KEY_PARAM_PATTERN, "$1[REDACTED]")
            .replace(GOOGLE_API_KEY_PATTERN, "[REDACTED]")
    }
}
