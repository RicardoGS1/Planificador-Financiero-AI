package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult

/**
 * Contrato para analizar comprobantes (imagen) o notas de voz (audio) con IA.
 */
interface ReceiptAnalysisRepository {

    suspend fun analyzeReceipt(
        imageBase64: String,
        expenseCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList()
    ): Result<ReceiptResult>

    suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType?,
        expenseCategoryNames: List<String> = emptyList(),
        incomeCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList(),
        mimeType: String = "audio/aac"
    ): Result<ReceiptResult>

    suspend fun analyzeSpreadsheet(
        spreadsheetText: String,
        startDateIso: String,
        endDateIso: String,
        expenseCategoryNames: List<String> = emptyList(),
        incomeCategoryNames: List<String> = emptyList(),
        accountNames: List<String> = emptyList()
    ): Result<ReceiptResult>
}
