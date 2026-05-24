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
import com.virtualworld.easyexpensecontrol.core.util.SensitiveDataSanitizer
import com.virtualworld.easyexpensecontrol.data.remote.dto.ReceiptLineItemDto
import retrofit2.HttpException
import java.io.IOException

/**
 * Origen de datos remoto: envía la imagen a Gemini y devuelve el DTO del comprobante.
 */
class ReceiptRemoteDataSource(
    private val geminiApi: GeminiApi,
    private val apiKey: String,
    private val gson: Gson,
    private val appContext: Context
) {

    private val fallbackExpenseCategories = "Supermercado, Transporte, Restaurante, Farmacia, Ocio, Gasolina, Hogar, Otros"
    private val fallbackIncomeCategories = "Salario, Freelance, Inversiones, Ventas, Reembolso, Regalo, Otros"

    private fun buildReceiptPrompt(categoryNames: List<String>): String {
        val categoryList = if (categoryNames.isNotEmpty()) {
            categoryNames.joinToString(", ")
        } else {
            fallbackExpenseCategories
        }
        val categoryInstruction = if (categoryNames.isNotEmpty()) {
            "DEBES elegir UNA de estas categorías exactamente: $categoryList. Si ninguna encaja bien, como segunda opción puedes usar: $fallbackExpenseCategories."
        } else {
            "DEBES elegir UNA de estas categorías exactamente: $categoryList."
        }
        return """
            Eres un asistente que analiza fotos de comprobantes de compra (tickets, facturas).
            Extrae la información y responde en JSON válido con exactamente este formato:
            {
              "transactions": [
                {
                  "amount": número (importe de la línea, usar punto como decimal),
                  "description": string breve (máximo 50 caracteres),
                  "categoryName": string con una categoría de gasto
                }
              ]
            }
            Si el comprobante incluye productos o gastos de varias categorías, crea UNA transacción por categoría o por línea relevante.
            Si todo el ticket corresponde a una sola categoría, devuelve un array con un solo elemento.
            La suma de los importes debe coincidir con el total del comprobante cuando sea posible.
            $categoryInstruction
            Si no puedes leer un importe, usa 0.0. Responde solo con el JSON, sin markdown ni texto adicional.
        """.trimIndent()
    }

    private fun buildAudioPrompt(type: TransactionType, categoryNames: List<String>): String {
        val typeLabelEs = if (type == TransactionType.Gasto) "gasto" else "ingreso"
        val fallback = if (type == TransactionType.Gasto) fallbackExpenseCategories else fallbackIncomeCategories
        val categoryList = if (categoryNames.isNotEmpty()) categoryNames.joinToString(", ") else fallback
        val categoryInstruction = if (categoryNames.isNotEmpty()) {
            "DEBES elegir UNA de estas categorías exactamente: $categoryList. Si ninguna encaja bien, como segunda opción puedes usar: $fallback."
        } else {
            "DEBES elegir UNA de estas categorías exactamente: $categoryList."
        }
        return """
            Eres un asistente que escucha una nota de voz del usuario describiendo uno o varios $typeLabelEs.
            El audio puede estar en cualquier idioma; entiende el contenido y extrae la información.
            Responde en JSON válido con exactamente este formato:
            {
              "transactions": [
                {
                  "amount": número (importe de la transacción, usar punto como decimal),
                  "description": string breve (máximo 50 caracteres),
                  "categoryName": string con la categoría del $typeLabelEs
                }
              ]
            }
            Si el usuario menciona varios $typeLabelEs de distintas categorías, crea UNA transacción por cada uno.
            Si solo describe un $typeLabelEs, devuelve un array con un solo elemento.
            $categoryInstruction
            Si no puedes determinar el importe, usa 0.0.
            Si no puedes determinar una descripción, deja "description" como string vacío.
            Responde solo con el JSON, sin markdown ni texto adicional.
        """.trimIndent()
    }

    suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String> = emptyList()): Result<List<ReceiptLineItemDto>> {
        val prompt = buildReceiptPrompt(categoryNames)
        return sendInlineDataRequest(prompt, "image/jpeg", imageBase64)
    }

    suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType,
        categoryNames: List<String> = emptyList(),
        mimeType: String = "audio/aac"
    ): Result<List<ReceiptLineItemDto>> {
        val prompt = buildAudioPrompt(type, categoryNames)
        return sendInlineDataRequest(prompt, mimeType, audioBase64)
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

            Log.d("GeminiResponse", "JSON recibido: $text")
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
                    isLegacySingleItem(obj) -> {
                        listOf(gson.fromJson(obj, ReceiptLineItemDto::class.java))
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
        return rawItems.map(::sanitizeLineItem)
    }

    private fun isLegacySingleItem(obj: JsonObject): Boolean =
        obj.has("amount") && obj.has("description") && obj.has("categoryName")

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

    private fun sanitizeLineItem(dto: ReceiptLineItemDto): ReceiptLineItemDto {
        val amount = dto.amount.coerceAtLeast(0.0)
        return dto.copy(
            amount = amount,
            description = dto.description.take(200),
            categoryName = dto.categoryName.take(100).ifEmpty {
                appContext.getString(R.string.category_default_other)
            }
        )
    }
}
