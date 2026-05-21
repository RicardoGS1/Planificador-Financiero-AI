package com.virtualworld.easyexpensecontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Línea individual devuelta por la IA al analizar un comprobante o audio.
 */
data class ReceiptLineItemDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("description") val description: String,
    @SerializedName("categoryName") val categoryName: String
)

/**
 * Respuesta JSON de la IA con una o varias transacciones detectadas.
 */
data class ReceiptAnalysisResponseDto(
    @SerializedName("transactions") val transactions: List<ReceiptLineItemDto>
)
