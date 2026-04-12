package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Calendar

@Composable
fun BudgetScreen(
    navController: NavHostController,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    transactionViewModel: TransactionViewModel
) {
    val currentCalendar = Calendar.getInstance()
    val currentMonth = (currentCalendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val expenseCategories = categoryViewModel
        .getCategoriesByType(TransactionType.Gasto)
        .collectAsState(initial = emptyList())
        .value
    val budgets = budgetViewModel.getAllBudgets.collectAsState(initial = emptyList()).value
    val currentMonthBudgets = budgets.filter { it.month.padStart(2, '0') == currentMonth && it.year == currentYear }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (expenseCategories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ScreenHeader(title = stringResource(R.string.screen_budget), showBackArrow = false)
                Text(
                    text = stringResource(R.string.budget_no_categories_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    ScreenHeader(title = stringResource(R.string.screen_budget), showBackArrow = false)
                    Spacer(Modifier.height(8.dp))
                }
                items(expenseCategories, key = { category -> category.id }) { category ->
                    val categoryBudget = currentMonthBudgets.find { it.category == category.id }
                    val spent = transactionViewModel
                        .getTransactionsByCategoryAndDate(category.id, currentYear, currentMonth)
                        .collectAsState(initial = emptyList())
                        .value
                        .sumOf { it.amount }
                    ExpenseBudgetItem(
                        category = category,
                        budget = categoryBudget,
                        spent = spent,
                        onActionClick = {
                            if (categoryBudget != null) {
                                navController.navigate(Screen.AddEditBudgetScreen.route + "/${categoryBudget.id}")
                            } else {
                                budgetViewModel.onBudgetCategoryChanged(category.id)
                                budgetViewModel.onBudgetMonthChanged(currentMonth)
                                budgetViewModel.onBudgetYearChanged(currentYear)
                                navController.navigate(Screen.AddEditBudgetScreen.route + "/0L")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddPresupuesto(
    onAddClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.displayCutout))
    ) {
        FilledTonalButton(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.add_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private val BudgetCardShape = RoundedCornerShape(16.dp)
private val BudgetProgressGreen = Color(0xFF4CAF50)
private val BudgetProgressRed = Color(0xFFE33936)

@Composable
fun ExpenseBudgetItem(
    category: Category,
    budget: Budget?,
    spent: Double,
    onActionClick: () -> Unit
) {
    val hasBudget = budget != null
    val limit = budget?.monthlyLimit ?: 0.0
    val remaining = limit - spent
    val isOverBudget = hasBudget && remaining < 0
    val spentFraction = if (hasBudget && limit > 0.0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onActionClick() },
        shape = BudgetCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name.ifEmpty { stringResource(R.string.no_category) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (hasBudget) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.budget_spent_limit,
                                spent,
                                limit
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) BudgetProgressRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOverBudget) {
                            Text(
                                text = stringResource(R.string.budget_attention),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BudgetProgressRed
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.budget_remaining, remaining),
                                style = MaterialTheme.typography.bodySmall,
                                color = BudgetProgressGreen
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BudgetSplitBar(spentFraction = spentFraction, isOverBudget = isOverBudget)
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasBudget) stringResource(R.string.increase_budget)
                        else stringResource(R.string.set_budget)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSplitBar(
    spentFraction: Float,
    isOverBudget: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (isOverBudget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BudgetProgressRed)
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(spentFraction.coerceIn(0f, 1f).coerceAtLeast(0.0001f))
                        .fillMaxSize()
                        .background(BudgetProgressRed)
                )
                Box(
                    modifier = Modifier
                        .weight((1f - spentFraction).coerceIn(0f, 1f).coerceAtLeast(0.0001f))
                        .fillMaxSize()
                        .background(BudgetProgressGreen)
                )
            }
        }
    }
}
