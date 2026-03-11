package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.remote.ReceiptRemoteDataSource
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository

/**
 * Implementación que delega en el data source remoto (Gemini).
 */
class ReceiptAnalysisRepositoryImpl(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptAnalysisRepository {

    override suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String>): Result<ReceiptResult> {
        return remoteDataSource.analyzeReceipt(imageBase64, categoryNames).map { dto ->
            ReceiptResult(
                amount = dto.amount,
                description = dto.description,
                suggestedCategoryName = dto.categoryName
            )
        }
    }
}
