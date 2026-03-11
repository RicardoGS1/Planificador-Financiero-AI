package com.virtualworld.easyexpensecontrol.domain.model

/**
 * Resultado del análisis de un comprobante por IA.
 */
data class ReceiptResult(
    val amount: Double,
    val description: String,
    val suggestedCategoryName: String
)
