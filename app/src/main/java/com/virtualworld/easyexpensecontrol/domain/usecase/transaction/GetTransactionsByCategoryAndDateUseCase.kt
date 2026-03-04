package com.virtualworld.easyexpensecontrol.domain.usecase.transaction

import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
class GetTransactionsByCategoryAndDateUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(categoryId: Long, year: Int, month: String): Flow<List<Transaction>> =
        transactionRepository.getTransactionsByCategoryAndDate(categoryId, year, month)
}
