package com.virtualworld.easyexpensecontrol.core.util

import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.ByteArrayInputStream

/**
 * Convierte un archivo .xlsx en texto tabular para enviarlo a la IA.
 */
object ExcelSpreadsheetReader {

    private const val MAX_ROWS_PER_SHEET = 500
    private const val MAX_SHEETS = 5
    private const val MAX_CHARS = 48_000

    fun readToText(bytes: ByteArray): Result<String> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("empty_file"))
        }
        return try {
            ByteArrayInputStream(bytes).use { input ->
                ReadableWorkbook(input).use { workbook ->
                    val builder = StringBuilder()
                    var sheetCount = 0
                    for (sheet in workbook.sheets) {
                        if (sheetCount >= MAX_SHEETS) break
                        builder.append("## Sheet: ").append(sheet.name).append('\n')
                        var rowNum = 0
                        sheet.openStream().use { rows ->
                            rows.forEach { row ->
                                if (rowNum >= MAX_ROWS_PER_SHEET) {
                                    builder.append("... (rows truncated)\n")
                                    return@forEach
                                }
                                builder.append(rowToLine(row)).append('\n')
                                rowNum++
                            }
                        }
                        builder.append('\n')
                        sheetCount++
                    }
                    val text = builder.toString().trim()
                    if (text.isBlank()) {
                        Result.failure(IllegalArgumentException("empty_spreadsheet"))
                    } else {
                        Result.success(text.take(MAX_CHARS))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rowToLine(row: Row): String {
        val cells = mutableListOf<String>()
        row.forEach { cell ->
            val value = cell.text.trim()
            if (value.isNotEmpty()) {
                cells.add(value.replace('|', '/'))
            }
        }
        return cells.joinToString(" | ")
    }
}
