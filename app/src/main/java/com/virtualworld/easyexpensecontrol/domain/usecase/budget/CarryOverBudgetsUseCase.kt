package com.virtualworld.easyexpensecontrol.domain.usecase.budget

import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CarryOverBudgetsUseCase(
    private val budgetRepository: BudgetRepository,
    private val getTransactionsByCategoryAndDateUseCase: GetTransactionsByCategoryAndDateUseCase
) {
    suspend operator fun invoke(month: String, year: Int) = withContext(Dispatchers.IO) {
        val normalizedMonth = month.padStart(2, '0')
        val currentMonthNum = normalizedMonth.toIntOrNull() ?: return@withContext

        val allBudgets = budgetRepository.getBudgets().first()

        val currentMonthCategoryIds = allBudgets
            .filter { it.month.padStart(2, '0') == normalizedMonth && it.year == year }
            .map { it.category }
            .toSet()

        val previousBudgets = allBudgets.filter { budget ->
            val budgetMonth = budget.month.padStart(2, '0').toIntOrNull() ?: 0
            budget.year < year || (budget.year == year && budgetMonth < currentMonthNum)
        }

        val latestBudgetByCategory = previousBudgets
            .groupBy { it.category }
            .mapValues { (_, budgets) -> budgets.maxBy { it.monthYearSortKey() } }

        latestBudgetByCategory
            .filter { (categoryId, _) -> categoryId !in currentMonthCategoryIds }
            .forEach { (categoryId, sourceBudget) ->
                val expenditure = getTransactionsByCategoryAndDateUseCase(
                    categoryId,
                    year,
                    normalizedMonth
                ).first().sumOf { it.amount }

                budgetRepository.addBudget(
                    Budget(
                        category = categoryId,
                        monthlyLimit = sourceBudget.monthlyLimit,
                        currentExpenditure = expenditure,
                        month = normalizedMonth,
                        year = year
                    )
                )
            }
    }

    private fun Budget.monthYearSortKey(): Int =
        year * 100 + (month.padStart(2, '0').toIntOrNull() ?: 0)
}
