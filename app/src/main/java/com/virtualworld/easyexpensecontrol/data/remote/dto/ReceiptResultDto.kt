package com.virtualworld.easyexpensecontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO devuelto por la IA al analizar un comprobante.
 */
data class ReceiptResultDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("description") val description: String,
    @SerializedName("categoryName") val categoryName: String
)
