package com.virtualworld.easyexpensecontrol.domain.usecase.budget

import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetByIdUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(id: Long): Flow<Budget> = budgetRepository.getBudgetById(id)
}
