package com.virtualworld.easyexpensecontrol.domain.usecase.receipt

import android.util.Base64
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetVisibleAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import kotlinx.coroutines.flow.first

class ProcessAudioUseCase(
    private val receiptAnalysisRepository: ReceiptAnalysisRepository,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase
) {

    suspend operator fun invoke(
        audioBytes: ByteArray,
        type: TransactionType?,
        mimeType: String = "audio/aac"
    ): Result<ReceiptResult> {
        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val expenseCategoryNames = getCategoriesByTypeUseCase(TransactionType.Gasto)
            .first()
            .map { it.name }
        val incomeCategoryNames = getCategoriesByTypeUseCase(TransactionType.Ingreso)
            .first()
            .map { it.name }
        val accountNames = getVisibleAccountsUseCase()
            .first()
            .map { it.name }
        return receiptAnalysisRepository.analyzeAudio(
            audioBase64 = base64,
            type = type,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames,
            mimeType = mimeType
        )
    }
}
