package com.virtualworld.easyexpensecontrol.viewmodel

enum class AiAnalysisSource {
    RECEIPT,
    AUDIO,
    SPREADSHEET
}

/**
 * Estado del análisis por IA (comprobante, audio o Excel).
 */
sealed class ReceiptProcessingState {
    data object Idle : ReceiptProcessingState()
    data object Loading : ReceiptProcessingState()
    data class Success(
        val transactionCount: Int,
        val source: AiAnalysisSource = AiAnalysisSource.RECEIPT
    ) : ReceiptProcessingState()
    data class Error(val message: String) : ReceiptProcessingState()
}
