package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import java.util.Calendar

@Composable
fun BudgetHistoryScreen(
    navController: NavHostController,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel
) {
    val budgets by budgetViewModel.getAllBudgets.collectAsState(initial = emptyList())
    val expenseCategories = categoryViewModel
        .getCategoriesByType(TransactionType.Gasto)
        .collectAsState(initial = emptyList())
        .value

    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    val monthNames = stringArrayResource(id = R.array.month_names_full)
    val oldBudgets = budgets
        .filter { budget ->
            val budgetMonth = budget.month.toIntOrNull() ?: 0
            budget.year < currentYear || (budget.year == currentYear && budgetMonth < currentMonth)
        }
        .sortedWith(compareByDescending<Budget> { it.year }.thenByDescending { it.month.toIntOrNull() ?: 0 })

    val groupedBudgets = oldBudgets.groupBy { MonthKey(year = it.year, month = it.month.toIntOrNull() ?: 0) }
    val sortedMonths = groupedBudgets.keys.sortedWith(compareByDescending<MonthKey> { it.year }.thenByDescending { it.month })

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.budget_history_title),
                    showBackArrow = true,
                    onBackClick = { navController.popBackStack() }
                )
                Spacer(Modifier.height(8.dp))
            }

            if (sortedMonths.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.budget_history_empty_message),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedMonths.forEach { monthKey ->
                    item(key = "header-${monthKey.year}-${monthKey.month}") {
                        val monthLabel = monthNames.getOrElse(monthKey.month - 1) { monthKey.month.toString() }
                        Text(
                            text = "$monthLabel ${monthKey.year}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    items(
                        items = groupedBudgets[monthKey].orEmpty(),
                        key = { budget -> budget.id }
                    ) { budget ->
                        val category = expenseCategories.find { it.id == budget.category }
                        category?.let {
                            ExpenseBudgetItem(
                                category = it,
                                budget = budget,
                                spent = budget.currentExpenditure,
                                onActionClick = {},
                                isActionEnabled = false,
                                showActionButton = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MonthKey(
    val year: Int,
    val month: Int
)
