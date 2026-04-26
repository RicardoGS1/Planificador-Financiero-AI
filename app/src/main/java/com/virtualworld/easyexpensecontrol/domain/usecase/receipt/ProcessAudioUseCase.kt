package com.virtualworld.easyexpensecontrol.domain.usecase.receipt

import android.util.Base64
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import kotlinx.coroutines.flow.first

/**
 * Caso de uso: analizar una nota de voz del usuario y obtener importe, descripción y categoría
 * sugerida. Incluye en el prompt las categorías existentes del tipo de transacción seleccionado
 * (gasto o ingreso) para que la IA elija una de la lista.
 */
class ProcessAudioUseCase(
    private val receiptAnalysisRepository: ReceiptAnalysisRepository,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase
) {

    suspend operator fun invoke(
        audioBytes: ByteArray,
        type: TransactionType,
        mimeType: String = "audio/aac"
    ): Result<ReceiptResult> {
        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val categoryNames = getCategoriesByTypeUseCase(type)
            .first()
            .map { it.name }
        return receiptAnalysisRepository.analyzeAudio(base64, type, categoryNames, mimeType)
    }
}
