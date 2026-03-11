package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult

/**
 * Contrato para analizar una imagen de comprobante con IA.
 * @param categoryNames categorías de gasto de la BD para que la IA elija una de la lista.
 */
interface ReceiptAnalysisRepository {

    suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String> = emptyList()): Result<ReceiptResult>
}
