package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.data.remote.ReceiptRemoteDataSource
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptLineItem
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository

/**
 * Implementación que delega en el data source remoto (Gemini).
 */
class ReceiptAnalysisRepositoryImpl(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptAnalysisRepository {

    override suspend fun analyzeReceipt(imageBase64: String, categoryNames: List<String>): Result<ReceiptResult> {
        return remoteDataSource.analyzeReceipt(imageBase64, categoryNames).map { items ->
            ReceiptResult(items = items.map(::toDomainLineItem))
        }
    }

    override suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType,
        categoryNames: List<String>,
        mimeType: String
    ): Result<ReceiptResult> {
        return remoteDataSource.analyzeAudio(audioBase64, type, categoryNames, mimeType).map { items ->
            ReceiptResult(items = items.map(::toDomainLineItem))
        }
    }

    private fun toDomainLineItem(dto: com.virtualworld.easyexpensecontrol.data.remote.dto.ReceiptLineItemDto) =
        ReceiptLineItem(
            amount = dto.amount,
            description = dto.description,
            suggestedCategoryName = dto.categoryName
        )
}
