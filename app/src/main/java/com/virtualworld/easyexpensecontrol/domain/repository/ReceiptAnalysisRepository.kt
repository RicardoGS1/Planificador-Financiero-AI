package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult

/**
 * Contrato para analizar comprobantes (imagen) o notas de voz (audio) con IA.
 */
interface ReceiptAnalysisRepository {

    /**
     * Analiza la imagen de un comprobante de gasto.
     * @param categoryNames categorías de gasto de la BD para que la IA elija una de la lista.
     */
    suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String> = emptyList()): Result<ReceiptResult>

    /**
     * Analiza un audio descrito por el usuario para extraer importe, descripción y categoría.
     * @param type tipo de transacción (Gasto/Ingreso) para adaptar el prompt y las categorías.
     * @param categoryNames categorías existentes del tipo indicado.
     * @param mimeType formato del audio enviado (por defecto AAC).
     */
    suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType,
        categoryNames: List<String> = emptyList(),
        mimeType: String = "audio/aac"
    ): Result<ReceiptResult>
}
