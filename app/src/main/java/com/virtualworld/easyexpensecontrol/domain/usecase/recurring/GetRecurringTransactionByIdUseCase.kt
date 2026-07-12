package com.virtualworld.easyexpensecontrol.domain.usecase.recurring

import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import com.virtualworld.easyexpensecontrol.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GetRecurringTransactionByIdUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(id: Long): RecurringTransaction? =
        recurringTransactionRepository.getById(id).firstOrNull()

    fun observe(id: Long): Flow<RecurringTransaction?> =
        recurringTransactionRepository.getById(id)
}
