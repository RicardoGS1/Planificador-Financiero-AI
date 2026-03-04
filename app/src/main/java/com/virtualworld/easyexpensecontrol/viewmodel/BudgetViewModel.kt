package com.virtualworld.easyexpensecontrol.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.AddBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.DeleteBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetForCategoryMonthAndYearUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.UpdateBudgetUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class BudgetViewModel(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val getBudgetForCategoryMonthAndYearUseCase: GetBudgetForCategoryMonthAndYearUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase
) : ViewModel() {
    var budgetCategoryState by mutableLongStateOf(0L)
    var budgetMonthlyLimitState by mutableDoubleStateOf(0.0)
    var budgetCurrentExpenditureState by mutableDoubleStateOf(0.0)

    private val currentMoment = Clock.System.now()
    private val localDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())

    private val currentMonth = localDateTime.monthNumber.toString()
    private val currentYear = localDateTime.year

    var budgetMonthState by mutableStateOf(currentMonth)
    var budgetYearState by mutableIntStateOf(currentYear)

    fun onBudgetCategoryChanged(newCategory: Long) {
        budgetCategoryState = newCategory
    }

    fun onBudgetMonthlyLimitChanged(newMonthlyLimit: Double) {
        budgetMonthlyLimitState = newMonthlyLimit
    }

    fun onBudgetMonthChanged(newMonth: String) {
        budgetMonthState = newMonth
    }

    fun onBudgetYearChanged(newYear: Int) {
        budgetYearState = newYear
    }

    val getAllBudgets = getBudgetsUseCase()

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            addBudgetUseCase(budget)
        }
    }

    fun getBudgetById(id: Long): Flow<Budget> = getBudgetByIdUseCase(id)

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            updateBudgetUseCase(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            deleteBudgetUseCase(budget)
        }
    }

    fun getBudgetForCategoryMonthAndYear(categoryId: Long, month: String, year: Int) =
        getBudgetForCategoryMonthAndYearUseCase(categoryId, month, year)
}
