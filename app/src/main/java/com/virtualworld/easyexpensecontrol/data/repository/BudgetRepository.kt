package com.virtualworld.easyexpensecontrol.data.repository

import com.virtualworld.easyexpensecontrol.data.local.BudgetDao
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository as BudgetRepositoryDomain
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) : BudgetRepositoryDomain {

    override suspend fun addBudget(budget: Budget) {
        budgetDao.addBudget(budget)
    }

    override fun getBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    override fun getBudgetById(id: Long): Flow<Budget> {
        return budgetDao.getBudgetById(id)
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    override fun getBudgetForCategoryMonthAndYear(categoryId: Long, month: String, year: Int): Flow<Budget?> {
        return budgetDao.getBudgetForCategoryMonthAndYear(categoryId, month, year)
    }
}
