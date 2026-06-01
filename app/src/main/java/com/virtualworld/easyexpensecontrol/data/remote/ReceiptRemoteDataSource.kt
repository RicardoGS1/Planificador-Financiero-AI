package com.virtualworld.easyexpensecontrol.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiContent
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiGenerationConfig
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiInlineData
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiPart
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiRequest
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.AiPromptBuilder
import com.virtualworld.easyexpensecontrol.core.util.LocaleHelper
import com.virtualworld.easyexpensecontrol.core.util.SensitiveDataSanitizer
import com.virtualworld.easyexpensecontrol.data.remote.dto.ReceiptLineItemDto
import retrofit2.HttpException
import java.io.IOException

/**
 * Origen de datos remoto: envía imagen o audio a Gemini y devuelve transacciones detectadas.
 */
class ReceiptRemoteDataSource(
    private val geminiApi: GeminiApi,
    private val apiKey: String,
    private val gson: Gson,
    private val appContext: Context
) {

    suspend fun analyzeReceipt(
        imageBase64: String,
        expenseCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList()
    ): Result<List<ReceiptLineItemDto>> {
        val prompt = buildPrompt(
            mode = AiPromptBuilder.InputMode.RECEIPT,
            transactionType = TransactionType.Gasto,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = emptyList(),
            accountNames = accountNames
        )
        return sendInlineDataRequest(prompt, "image/jpeg", imageBase64)
    }

    suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType?,
        expenseCategoryNames: List<String> = emptyList(),
        incomeCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList(),
        mimeType: String = "audio/aac"
    ): Result<List<ReceiptLineItemDto>> {
        val prompt = buildPrompt(
            mode = AiPromptBuilder.InputMode.AUDIO,
            transactionType = type,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames
        )
        return sendInlineDataRequest(prompt, mimeType, audioBase64)
    }

    suspend fun analyzeSpreadsheet(
        spreadsheetText: String,
        startDateIso: String,
        endDateIso: String,
        expenseCategoryNames: List<String> = emptyList(),
        incomeCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList()
    ): Result<List<ReceiptLineItemDto>> {
        val prompt = buildPrompt(
            mode = AiPromptBuilder.InputMode.EXCEL,
            transactionType = null,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames,
            spreadsheetStartDateIso = startDateIso,
            spreadsheetEndDateIso = endDateIso
        )
        val fullPrompt = """
            $prompt

            --- SPREADSHEET DATA ---
            $spreadsheetText
        """.trimIndent()
        return sendTextRequest(fullPrompt)
    }

    suspend fun analyzePdf(
        pdfBase64: String,
        startDateIso: String,
        endDateIso: String,
        expenseCategoryNames: List<String> = emptyList(),
        incomeCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList()
    ): Result<List<ReceiptLineItemDto>> {
        val prompt = buildPrompt(
            mode = AiPromptBuilder.InputMode.PDF,
            transactionType = null,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames,
            spreadsheetStartDateIso = startDateIso,
            spreadsheetEndDateIso = endDateIso
        )
        return sendInlineDataRequest(prompt, "application/pdf", pdfBase64)
    }

    private fun buildPrompt(
        mode: AiPromptBuilder.InputMode,
        transactionType: TransactionType?,
        expenseCategoryNames: List<String>,
        incomeCategoryNames: List<String>,
        accountNames: List<String>,
        spreadsheetStartDateIso: String? = null,
        spreadsheetEndDateIso: String? = null
    ): String {
        val localizedContext = LocaleHelper.applySavedLocale(appContext)
        val effectiveLocale = LocaleHelper.getEffectiveLocale(appContext)
        val outputLanguageTag = AiPromptBuilder.resolveOutputLanguageTag(
            contextTag = LocaleHelper.getSavedLanguageTag(appContext),
            fallbackLocale = effectiveLocale
        )
        val defaultOtherCategory = localizedContext.getString(R.string.category_default_other)
        return AiPromptBuilder.buildPrompt(
            mode = mode,
            transactionType = transactionType,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames,
            outputLanguageTag = outputLanguageTag,
            defaultOtherCategory = defaultOtherCategory,
            spreadsheetStartDateIso = spreadsheetStartDateIso,
            spreadsheetEndDateIso = spreadsheetEndDateIso
        )
    }

    private suspend fun sendTextRequest(prompt: String): Result<List<ReceiptLineItemDto>> {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json")
            )
            val response = geminiApi.generateContent(apiKey, request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return Result.failure(
                    IllegalStateException(appContext.getString(R.string.gemini_empty_response))
                )

            Log.d(TAG, "JSON recibido (excel): $text")
            val items = parseReceiptJson(text)
            if (items.isEmpty()) {
                return Result.failure(
                    IllegalStateException(appContext.getString(R.string.gemini_empty_response))
                )
            }
            Result.success(items)
        } catch (e: JsonSyntaxException) {
            Result.failure(
                IllegalArgumentException(appContext.getString(R.string.gemini_parse_error), e)
            )
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty()
            val msg = sanitizeUserMessage(buildHttpErrorMessage(e.code(), body))
            Result.failure(IOException(msg))
        } catch (e: Exception) {
            Result.failure(
                IOException(
                    sanitizeUserMessage(
                        e.message ?: appContext.getString(R.string.error_spreadsheet_analysis)
                    )
                )
            )
        }
    }

    private suspend fun sendInlineDataRequest(
        prompt: String,
        mimeType: String,
        dataBase64: String
    ): Result<List<ReceiptLineItemDto>> {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = mimeType,
                                    data = dataBase64
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json")
            )
            val response = geminiApi.generateContent(apiKey, request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: return Result.failure(
                    IllegalStateException(appContext.getString(R.string.gemini_empty_response))
                )

            Log.d(TAG, "JSON recibido: $text")
            val items = parseReceiptJson(text)
            if (items.isEmpty()) {
                return Result.failure(
                    IllegalStateException(appContext.getString(R.string.gemini_empty_response))
                )
            }
            Result.success(items)
        } catch (e: JsonSyntaxException) {
            Result.failure(
                IllegalArgumentException(appContext.getString(R.string.gemini_parse_error), e)
            )
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty()
            val msg = sanitizeUserMessage(buildHttpErrorMessage(e.code(), body))
            Result.failure(IOException(msg))
        } catch (e: Exception) {
            Result.failure(
                IOException(
                    sanitizeUserMessage(
                        e.message ?: appContext.getString(R.string.error_receipt_analysis)
                    )
                )
            )
        }
    }

    private fun parseReceiptJson(jsonText: String): List<ReceiptLineItemDto> {
        val normalized = jsonText.trim().removeSurrounding("```json", "```").trim()
        val element = gson.fromJson(normalized, JsonElement::class.java)
        val rawItems = when {
            element.isJsonArray -> {
                gson.fromJson(element, Array<ReceiptLineItemDto>::class.java).toList()
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when {
                    obj.has("transactions") && obj.get("transactions").isJsonArray -> {
                        gson.fromJson(obj.get("transactions"), Array<ReceiptLineItemDto>::class.java).toList()
                    }
                    obj.has("amount") && obj.has("description") && obj.has("categoryName") -> {
                        listOf(gson.fromJson(obj, ReceiptLineItemDto::class.java))
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
        return rawItems.map(::sanitizeLineItem)
    }

    private fun buildHttpErrorMessage(code: Int, body: String): String {
        val apiDetail = parseApiErrorMessage(body)
        return when (code) {
            403 -> appContext.getString(R.string.gemini_error_403)
            401 -> appContext.getString(R.string.gemini_error_401)
            429 -> appContext.getString(R.string.gemini_error_429)
            else -> appContext.getString(
                R.string.gemini_error_other,
                code,
                apiDetail ?: appContext.getString(R.string.error_unknown)
            )
        }
    }

    private fun parseApiErrorMessage(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val root = gson.fromJson(body, JsonObject::class.java)
            root.getAsJsonObject("error")?.get("message")?.asString?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeUserMessage(message: String): String =
        SensitiveDataSanitizer.sanitize(message, listOf(apiKey))

    private fun sanitizeLineItem(dto: ReceiptLineItemDto): ReceiptLineItemDto =
        dto.copy(
            amount = dto.amount.coerceAtLeast(0.0),
            description = dto.description.trim().take(50),
            categoryName = dto.categoryName.trim().take(30),
            date = dto.date.trim(),
            transactionType = dto.transactionType.trim(),
            accountName = dto.accountName.trim().take(50)
        )

    private companion object {
        const val TAG = "ReceiptRemoteDataSource"
    }
}
