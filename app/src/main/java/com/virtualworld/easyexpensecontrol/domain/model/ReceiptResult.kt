package com.virtualworld.easyexpensecontrol.domain.model

/**
 * Línea individual detectada por IA en un comprobante o nota de voz.
 */
data class ReceiptLineItem(
    val amount: Double,
    val description: String,
    val suggestedCategoryName: String
)

/**
 * Resultado del análisis de un comprobante o audio por IA.
 */
data class ReceiptResult(
    val items: List<ReceiptLineItem>
)
