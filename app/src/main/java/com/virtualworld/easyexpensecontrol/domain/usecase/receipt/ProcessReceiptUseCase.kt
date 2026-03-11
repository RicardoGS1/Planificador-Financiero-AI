package com.virtualworld.easyexpensecontrol.domain.usecase.receipt

import android.util.Base64
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import kotlinx.coroutines.flow.first

/**
 * Caso de uso: analizar imagen de comprobante y obtener importe, descripción y categoría sugerida.
 * Incluye las categorías de gasto de la BD en el prompt para que la IA elija una de la lista.
 */
class ProcessReceiptUseCase(
    private val receiptAnalysisRepository: ReceiptAnalysisRepository,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase
) {

    suspend operator fun invoke(imageBytes: ByteArray): Result<ReceiptResult> {
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val categoryNames = getCategoriesByTypeUseCase(TransactionType.Gasto)
            .first()
            .map { it.name }
        return receiptAnalysisRepository.analyzeReceipt(base64, categoryNames)
    }
}
