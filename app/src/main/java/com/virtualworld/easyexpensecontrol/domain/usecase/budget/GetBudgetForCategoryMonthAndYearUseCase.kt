package com.virtualworld.easyexpensecontrol.domain.usecase.budget

import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetForCategoryMonthAndYearUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(categoryId: Long, month: String, year: Int): Flow<Budget?> =
        budgetRepository.getBudgetForCategoryMonthAndYear(categoryId, month, year)
}
