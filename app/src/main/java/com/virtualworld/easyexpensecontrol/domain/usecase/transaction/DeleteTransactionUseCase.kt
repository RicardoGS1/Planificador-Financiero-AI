package com.virtualworld.easyexpensecontrol.domain.usecase.transaction

import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction)
        val count = transactionRepository.getTransactionCountForCategory(transaction.category)
        if (count == 0) {
            categoryRepository.deleteCategory(categoryRepository.getCategoryById(transaction.category).first())
        }
    }
}
