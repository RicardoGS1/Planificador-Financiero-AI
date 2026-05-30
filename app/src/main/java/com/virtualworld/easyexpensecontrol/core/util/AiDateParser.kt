package com.virtualworld.easyexpensecontrol.core.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

object AiDateParser {

    fun parseIsoDateOrNull(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val localDate = LocalDate.parse(trimmed)
            localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
