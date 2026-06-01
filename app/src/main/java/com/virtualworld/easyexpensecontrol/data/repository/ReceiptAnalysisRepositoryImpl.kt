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

    override suspend fun analyzeReceipt(
        imageBase64: String,
        expenseCategoryNames: List<String>,
        accountNames: List<String>
    ): Result<ReceiptResult> {
        return remoteDataSource.analyzeReceipt(imageBase64, expenseCategoryNames, accountNames).map { items ->
            ReceiptResult(items = items.map(::toDomainLineItem))
        }
    }

    override suspend fun analyzeAudio(
        audioBase64: String,
        type: TransactionType?,
        expenseCategoryNames: List<String>,
        incomeCategoryNames: List<String>,
        accountNames: List<String>,
        mimeType: String
    ): Result<ReceiptResult> {
        return remoteDataSource.analyzeAudio(
            audioBase64,
            type,
            expenseCategoryNames,
            incomeCategoryNames,
            accountNames,
            mimeType
        ).map { items ->
            ReceiptResult(items = items.map(::toDomainLineItem))
        }
    }

    override suspend fun analyzeSpreadsheet(
        spreadsheetText: String,
        startDateIso: String,
        endDateIso: String,
        expenseCategoryNames: List<String>,
        incomeCategoryNames: List<String>,
        accountNames: List<String>
    ): Result<ReceiptResult> {
        return remoteDataSource.analyzeSpreadsheet(
            spreadsheetText,
            startDateIso,
            endDateIso,
            expenseCategoryNames,
            incomeCategoryNames,
            accountNames
        ).map { items ->
            ReceiptResult(items = items.map(::toDomainLineItem))
        }
    }

    private fun toDomainLineItem(dto: com.virtualworld.easyexpensecontrol.data.remote.dto.ReceiptLineItemDto) =
        ReceiptLineItem(
            amount = dto.amount,
            description = dto.description,
            suggestedCategoryName = dto.categoryName,
            suggestedDateIso = dto.date,
            suggestedTransactionType = dto.transactionType,
            suggestedAccountName = dto.accountName
        )
}
