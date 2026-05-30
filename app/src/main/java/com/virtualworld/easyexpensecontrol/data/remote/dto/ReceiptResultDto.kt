package com.virtualworld.easyexpensecontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Línea individual devuelta por la IA al analizar un comprobante o audio.
 */
data class ReceiptLineItemDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("description") val description: String,
    @SerializedName("categoryName") val categoryName: String,
    @SerializedName("date") val date: String = "",
    @SerializedName("transactionType") val transactionType: String = "",
    @SerializedName("accountName") val accountName: String = ""
)

/**
 * Respuesta JSON de la IA con una o varias transacciones detectadas.
 */
data class ReceiptAnalysisResponseDto(
    @SerializedName("transactions") val transactions: List<ReceiptLineItemDto>
)
