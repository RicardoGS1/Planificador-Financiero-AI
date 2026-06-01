package com.virtualworld.easyexpensecontrol.domain.usecase.receipt

import com.virtualworld.easyexpensecontrol.core.util.ExcelSpreadsheetReader
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetVisibleAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import kotlinx.coroutines.flow.first

class ProcessSpreadsheetUseCase(
    private val receiptAnalysisRepository: ReceiptAnalysisRepository,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase
) {

    suspend operator fun invoke(
        fileBytes: ByteArray,
        startDateIso: String,
        endDateIso: String
    ): Result<ReceiptResult> {
        val spreadsheetText = ExcelSpreadsheetReader.readToText(fileBytes).getOrElse { return Result.failure(it) }
        val expenseCategoryNames = getCategoriesByTypeUseCase(TransactionType.Gasto)
            .first()
            .map { it.name }
        val incomeCategoryNames = getCategoriesByTypeUseCase(TransactionType.Ingreso)
            .first()
            .map { it.name }
        val accountNames = getVisibleAccountsUseCase()
            .first()
            .map { it.name }
        return receiptAnalysisRepository.analyzeSpreadsheet(
            spreadsheetText = spreadsheetText,
            startDateIso = startDateIso,
            endDateIso = endDateIso,
            expenseCategoryNames = expenseCategoryNames,
            incomeCategoryNames = incomeCategoryNames,
            accountNames = accountNames
        )
    }
}
