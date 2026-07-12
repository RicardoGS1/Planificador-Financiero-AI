package com.virtualworld.easyexpensecontrol.domain.usecase.recurring

import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import com.virtualworld.easyexpensecontrol.domain.repository.RecurringTransactionRepository

class UpdateRecurringTransactionUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurringTransaction: RecurringTransaction) {
        recurringTransactionRepository.update(recurringTransaction)
    }
}
