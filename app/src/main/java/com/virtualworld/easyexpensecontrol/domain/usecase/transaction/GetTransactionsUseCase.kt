package com.virtualworld.easyexpensecontrol.domain.usecase.transaction

import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = transactionRepository.getTransactions()
}
