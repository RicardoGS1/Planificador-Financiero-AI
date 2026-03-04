package com.virtualworld.easyexpensecontrol.domain.usecase.budget

import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) = withContext(Dispatchers.IO) {
        budgetRepository.updateBudget(budget)
    }
}
