package com.virtualworld.easyexpensecontrol.domain.repository

import com.virtualworld.easyexpensecontrol.data.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    suspend fun addBudget(budget: Budget)

    fun getBudgets(): Flow<List<Budget>>

    fun getBudgetById(id: Long): Flow<Budget>

    suspend fun updateBudget(budget: Budget)

    suspend fun deleteBudget(budget: Budget)

    fun getBudgetForCategoryMonthAndYear(categoryId: Long, month: String, year: Int): Flow<Budget?>
}
