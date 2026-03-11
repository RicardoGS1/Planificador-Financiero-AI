package com.virtualworld.easyexpensecontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Estructura de request para la API Gemini generateContent.
 */
data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("inlineData") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("data") val data: String
)

data class GeminiGenerationConfig(
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json"
)
