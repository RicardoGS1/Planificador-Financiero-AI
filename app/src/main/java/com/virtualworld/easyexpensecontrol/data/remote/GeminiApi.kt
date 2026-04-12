package com.virtualworld.easyexpensecontrol.data.remote

import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiRequest
import com.virtualworld.easyexpensecontrol.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaz Retrofit para la API Gemini (generateContent).
 * Base URL: https://generativelanguage.googleapis.com/v1beta/
 */
interface GeminiApi {

    //@POST("models/gemini-2.5-flash:generateContent")
    @POST("models/gemini-3.1-flash-lite-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}
