package com.virtualworld.easyexpensecontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Estructura de response de la API Gemini generateContent.
 */
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiCandidateContent?
)

data class GeminiCandidateContent(
    @SerializedName("parts") val parts: List<GeminiPartResponse>?
)

data class GeminiPartResponse(
    @SerializedName("text") val text: String?
)
