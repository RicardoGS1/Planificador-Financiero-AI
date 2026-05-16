package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddEditDetailBudgetView(
    id: Long,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    transactionViewModel: TransactionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (id != 0L) {
        val transaction = budgetViewModel.getBudgetById(id).collectAsState(initial = Budget(0L, 0L, 0.0, 0.0, "0", 0))
        transaction.value.let {
            budgetViewModel.budgetCategoryState = it.category
            budgetViewModel.budgetCurrentExpenditureState = it.currentExpenditure
            budgetViewModel.budgetMonthlyLimitState = it.monthlyLimit
            budgetViewModel.budgetMonthState = it.month
            budgetViewModel.budgetYearState = it.year
        }

        val category = categoryViewModel.getCategoryById(budgetViewModel.budgetCategoryState).collectAsState(initial = null)
        LaunchedEffect(category.value) {
            category.value?.let {
                categoryViewModel.categoryNameState = it.name
                categoryViewModel.categoryTypeState = it.type
            }
        }
    } else {
        // Mantiene la categoría preseleccionada cuando se navega desde "Fijar presupuesto"
        // y evita que se resetee en recomposiciones.
        budgetViewModel.budgetCurrentExpenditureState = 0.0
        budgetViewModel.budgetMonthlyLimitState = 0.0
    }

    fun handleSaveBudget() {
        scope.launch {
            try {
                val totalExpenditure = withContext(Dispatchers.IO) {
                    transactionViewModel
                        .getTransactionsByCategoryAndDate(
                            budgetViewModel.budgetCategoryState,
                            budgetViewModel.budgetYearState,
                            budgetViewModel.budgetMonthState.padStart(2, '0')
                        )
                        .first()
                        .sumOf { it.amount }
                }

                val budget = Budget(
                    id = if (id != 0L) id else 0L,
                    category = budgetViewModel.budgetCategoryState,
                    monthlyLimit = budgetViewModel.budgetMonthlyLimitState,
                    currentExpenditure = totalExpenditure,
                    month = budgetViewModel.budgetMonthState.padStart(2, '0'),
                    year = budgetViewModel.budgetYearState
                )

                withContext(Dispatchers.IO) {
                    if (id != 0L) {
                        budgetViewModel.updateBudget(budget)
                    } else {
                        budgetViewModel.addBudget(budget)
                    }
                }

                snackbarHostState.showSnackbar(context.getString(R.string.success_operation))
                navController.navigateUp()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error_prefix, e.message ?: context.getString(R.string.error_unknown))
                )
            } finally {
                isLoading = false
            }
        }
    }

    val monthNamesFull = stringArrayResource(R.array.month_names_full)
    val displayDate = remember(budgetViewModel.budgetMonthState, budgetViewModel.budgetYearState, monthNamesFull) {
        val m = budgetViewModel.budgetMonthState.toIntOrNull() ?: 0
        val y = budgetViewModel.budgetYearState
        if (m in 1..12 && y > 0) "${monthNamesFull[m - 1]} $y" else ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = if (id != 0L) {
                    stringResource(id = R.string.update_budget)
                } else {
                    stringResource(id = R.string.add_budget)
                },
                showBackArrow = true,
                onBackClick = { navController.navigateUp() }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Categoría — Card con selector integrado
                BudgetCategoryCard(
                    selectedCategory = budgetViewModel.budgetCategoryState,
                    categoryViewModel = categoryViewModel,
                    onCategoryChanged = budgetViewModel::onBudgetCategoryChanged
                )

                // Mes y Año — Card con fila táctil (selector fuera del botón)
                var isPickerVisible by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPickerVisible = !isPickerVisible },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.month_year),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayDate.ifEmpty { stringResource(R.string.tap_pick_month_year) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (displayDate.isNotEmpty())
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isPickerVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isPickerVisible) stringResource(R.string.cd_hide_picker)
                                else stringResource(R.string.cd_change_month_year),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (isPickerVisible) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MonthPickerInline(
                                currentMonth = budgetViewModel.budgetMonthState.toIntOrNull()?.takeIf { it in 1..12 }
                                    ?: java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                                currentYear = budgetViewModel.budgetYearState.takeIf { it > 0 }
                                    ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                onSelected = { month, year ->
                                    budgetViewModel.onBudgetMonthChanged(month.toString())
                                    budgetViewModel.onBudgetYearChanged(year)
                                    isPickerVisible = false
                                }
                            )
                        }
                    }
                }

                // Límite mensual
                var monthlyLimitText by remember { mutableStateOf("") }
                LaunchedEffect(budgetViewModel.budgetMonthlyLimitState) {
                    val modelAmount = budgetViewModel.budgetMonthlyLimitState
                    val textAmount = monthlyLimitText.toDoubleOrNull() ?: 0.0
                    if (modelAmount != textAmount) {
                        monthlyLimitText = if (modelAmount > 0) {
                            if (modelAmount == modelAmount.toLong().toDouble())
                                modelAmount.toLong().toString()
                            else modelAmount.toString()
                        } else ""
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.monthly_limit),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            label = stringResource(R.string.hint_amount),
                            value = monthlyLimitText,
                            onValueChange = { value ->
                                val normalized = value.replace(',', '.')
                                val filtered = normalized.filter { it.isDigit() || it == '.' }
                                val sanitized = run {
                                    val firstDot = filtered.indexOf('.')
                                    if (firstDot == -1) filtered
                                    else filtered.substring(0, firstDot + 1) +
                                        filtered.substring(firstDot + 1).replace(".", "")
                                }
                                monthlyLimitText = sanitized
                                budgetViewModel.onBudgetMonthlyLimitChanged(
                                    sanitized.toDoubleOrNull() ?: 0.0
                                )
                            },
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                }

                // Botón principal
                FilledTonalButton(
                    onClick = {
                        when {
                            budgetViewModel.budgetCategoryState == 0L -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_select_category))
                                }
                            }
                            id == 0L && displayDate.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_select_month_year))
                                }
                            }
                            budgetViewModel.budgetMonthlyLimitState == 0.0 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_monthly_limit_empty))
                                }
                            }
                            id == 0L -> {
                                isLoading = true
                                scope.launch {
                                    val budgetAlreadyExists = budgetViewModel
                                        .getBudgetForCategoryMonthAndYear(
                                            budgetViewModel.budgetCategoryState,
                                            budgetViewModel.budgetMonthState,
                                            budgetViewModel.budgetYearState
                                        ).first()
                                    if (budgetAlreadyExists != null) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.err_duplicate_budget))
                                        isLoading = false
                                    } else {
                                        handleSaveBudget()
                                    }
                                }
                            }
                            else -> {
                                isLoading = true
                                handleSaveBudget()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Text(
                            text = if (id != 0L) stringResource(id = R.string.update_budget)
                            else stringResource(id = R.string.add_budget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MonthPickerInline(
    currentMonth: Int,
    currentYear: Int,
    onSelected: (Int, Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    val months = stringArrayResource(R.array.month_names_short).toList()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { selectedYear = (selectedYear - 1).coerceAtLeast(2020) }) {
            Icon(Icons.Default.ExpandMore, contentDescription = stringResource(R.string.cd_year_decrease), modifier = Modifier.size(28.dp))
        }
        Text(
            text = "$selectedYear",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = { selectedYear++ }) {
            Icon(Icons.Default.ExpandLess, contentDescription = stringResource(R.string.cd_year_increase), modifier = Modifier.size(28.dp))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to 3, 3 to 6, 6 to 9, 9 to 12).forEach { (start, end) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (index in start until end) {
                    val name = months[index]
                    val isSelected = (index + 1) == selectedMonth
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMonth = index + 1 },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    FilledTonalButton(
        onClick = { onSelected(selectedMonth, selectedYear) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(stringResource(R.string.apply_month_year))
    }
}

@Composable
private fun BudgetCategoryCard(
    selectedCategory: Long,
    categoryViewModel: CategoryViewModel,
    onCategoryChanged: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = categoryViewModel.getCategoriesByType(TransactionType.Gasto).collectAsState(initial = emptyList()).value
    val selectedCategoryName = categories.find { it.id == selectedCategory }?.name
        ?: stringResource(R.string.select_category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = selectedCategoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedCategory != 0L)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.cd_pick_category),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(text = category.name) },
                            onClick = {
                                onCategoryChanged(category.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetCategoryDropdown(
    selectedCategory: Long,
    categoryViewModel: CategoryViewModel,
    onCategoryChanged: (Long) -> Unit
) {
    BudgetCategoryCard(
        selectedCategory = selectedCategory,
        categoryViewModel = categoryViewModel,
        onCategoryChanged = onCategoryChanged
    )
}
