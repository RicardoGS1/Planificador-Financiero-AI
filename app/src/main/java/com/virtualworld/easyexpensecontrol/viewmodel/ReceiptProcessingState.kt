package com.virtualworld.easyexpensecontrol.viewmodel

/**
 * Estado del análisis de comprobante por IA.
 */
sealed class ReceiptProcessingState {
    data object Idle : ReceiptProcessingState()
    data object Loading : ReceiptProcessingState()
    data class Success(val categoryNameForUi: String) : ReceiptProcessingState()
    data class Error(val message: String) : ReceiptProcessingState()
}
