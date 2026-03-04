package com.virtualworld.easyexpensecontrol.domain.usecase.transaction

import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
class GetTransactionByIdUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(id: Long): Flow<Transaction> = transactionRepository.getTransactionById(id)
}
