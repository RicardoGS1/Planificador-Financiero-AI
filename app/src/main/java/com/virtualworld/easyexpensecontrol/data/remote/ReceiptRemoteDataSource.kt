package com.virtualworld.easyexpensecontrol.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiContent
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiGenerationConfig
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiInlineData
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiPart
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiRequest
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.remote.dto.ReceiptResultDto
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
            Extrae ÚNICAMENTE la información solicitada y responde en JSON válido con exactamente estos tres campos:
            - "amount": número (importe total del comprobante, usar punto como decimal).
            - "description": string breve que describa la compra (máximo 50 caracteres).
            - "categoryName": string con una categoría de gasto. $categoryInstruction
            Si no puedes leer el importe, usa 0.0. Responde solo con el JSON, sin markdown ni texto adicional.
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
            Eres un asistente que escucha una nota de voz del usuario describiendo un $typeLabelEs.
            El audio puede estar en cualquier idioma; entiende el contenido y extrae la información.
            Responde en JSON válido con exactamente estos tres campos:
            - "amount": número (importe del $typeLabelEs, usar punto como decimal).
            - "description": string breve que describa el $typeLabelEs (máximo 50 caracteres).
            - "categoryName": string con la categoría del $typeLabelEs. $categoryInstruction
            Si no puedes determinar el importe, usa 0.0.
            Si no puedes determinar una descripción, deja "description" como string vacío.
            Responde solo con el JSON, sin markdown ni texto adicional.
        """.trimIndent()
    }

    suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String> = emptyList()): Result<ReceiptResultDto> {
        val prompt = buildReceiptPrompt(categoryNames)
        return sendInlineDataRequest(prompt, "image/jpeg", imageBase64)
    }

    suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType,
        categoryNames: List<String> = emptyList(),
        mimeType: String = "audio/aac"
    ): Result<ReceiptResultDto> {
        val prompt = buildAudioPrompt(type, categoryNames)
        return sendInlineDataRequest(prompt, mimeType, audioBase64)
    }

    private suspend fun sendInlineDataRequest(
        prompt: String,
        mimeType: String,
        dataBase64: String
    ): Result<ReceiptResultDto> {
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
            val dto = parseReceiptJson(text)
            Result.success(dto)
        } catch (e: JsonSyntaxException) {
            Result.failure(
                IllegalArgumentException(appContext.getString(R.string.gemini_parse_error), e)
            )
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: ""
            val msg = when (e.code()) {
                403 -> appContext.getString(R.string.gemini_error_403) +
                    if (body.isNotEmpty()) " ${body}" else ""
                401 -> appContext.getString(R.string.gemini_error_401)
                429 -> appContext.getString(R.string.gemini_error_429)
                else -> appContext.getString(
                    R.string.gemini_error_other,
                    e.code(),
                    body.ifEmpty { e.message() ?: "" }
                )
            }
            Result.failure(IOException(msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseReceiptJson(jsonText: String): ReceiptResultDto {
        val normalized = jsonText.trim().removeSurrounding("```json", "```").trim()
        val dto = gson.fromJson(normalized, ReceiptResultDto::class.java)
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
