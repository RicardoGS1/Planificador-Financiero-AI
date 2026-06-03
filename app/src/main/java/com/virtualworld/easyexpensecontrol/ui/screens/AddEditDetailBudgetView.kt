package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BudgetFormCardShape = RoundedCornerShape(20.dp)

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

    LaunchedEffect(id) {
        if (id != 0L) {
            val budget = budgetViewModel.getBudgetById(id).first()
            budgetViewModel.budgetCategoryState = budget.category
            budgetViewModel.budgetCurrentExpenditureState = budget.currentExpenditure
            budgetViewModel.budgetMonthlyLimitState = budget.monthlyLimit
            budgetViewModel.budgetMonthState = budget.month
            budgetViewModel.budgetYearState = budget.year
            categoryViewModel.getCategoryById(budget.category).first()?.let { category ->
                categoryViewModel.categoryNameState = category.name
                categoryViewModel.categoryTypeState = category.type
            }
        } else {
            budgetViewModel.budgetCurrentExpenditureState = 0.0
            budgetViewModel.budgetMonthlyLimitState = 0.0
        }
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

                isLoading = false
                snackbarHostState.showSnackbar(context.getString(R.string.success_operation))
                navController.navigateUp()
            } catch (e: Exception) {
                isLoading = false
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error_prefix, e.message ?: context.getString(R.string.error_unknown))
                )
            }
        }
    }

    val monthNamesFull = stringArrayResource(R.array.month_names_full)
    val displayDate = remember(budgetViewModel.budgetMonthState, budgetViewModel.budgetYearState, monthNamesFull) {
        val m = budgetViewModel.budgetMonthState.toIntOrNull() ?: 0
        val y = budgetViewModel.budgetYearState
        if (m in 1..12 && y > 0) "${monthNamesFull[m - 1]} $y" else ""
    }

    val categories = categoryViewModel.getCategoriesByType(TransactionType.Gasto).collectAsState(initial = emptyList()).value

    AddEditDetailBudgetContent(
        id = id,
        isLoading = isLoading,
        displayDate = displayDate,
        budgetCategoryState = budgetViewModel.budgetCategoryState,
        budgetMonthlyLimitState = budgetViewModel.budgetMonthlyLimitState,
        budgetMonthState = budgetViewModel.budgetMonthState,
        budgetYearState = budgetViewModel.budgetYearState,
        categories = categories,
        onBackClick = { navController.navigateUp() },
        onCategoryChanged = budgetViewModel::onBudgetCategoryChanged,
        onMonthYearSelected = { month, year ->
            budgetViewModel.onBudgetMonthChanged(month.toString())
            budgetViewModel.onBudgetYearChanged(year)
        },
        onMonthlyLimitChanged = { budgetViewModel.onBudgetMonthlyLimitChanged(it) },
        onSaveClick = {
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
                            isLoading = false
                            snackbarHostState.showSnackbar(context.getString(R.string.err_duplicate_budget))
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
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun AddEditDetailBudgetContent(
    id: Long,
    isLoading: Boolean,
    displayDate: String,
    budgetCategoryState: Long,
    budgetMonthlyLimitState: Double,
    budgetMonthState: String,
    budgetYearState: Int,
    categories: List<Category>,
    onBackClick: () -> Unit,
    onCategoryChanged: (Long) -> Unit,
    onMonthYearSelected: (Int, Int) -> Unit,
    onMonthlyLimitChanged: (Double) -> Unit,
    onSaveClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollState = rememberScrollState()

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
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BudgetCategoryCard(
                    selectedCategory = budgetCategoryState,
                    categories = categories,
                    onCategoryChanged = onCategoryChanged
                )

                var isPickerVisible by remember { mutableStateOf(false) }
                BudgetFormCard(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.month_year),
                    subtitle = displayDate.ifEmpty { stringResource(R.string.tap_pick_month_year) },
                    subtitleEmphasis = displayDate.isNotEmpty(),
                    onClick = { isPickerVisible = !isPickerVisible },
                    trailingIcon = if (isPickerVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                ) {
                    if (isPickerVisible) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MonthPickerInline(
                            currentMonth = budgetMonthState.toIntOrNull()?.takeIf { it in 1..12 }
                                ?: java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                            currentYear = budgetYearState.takeIf { it > 0 }
                                ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                            onSelected = { month, year ->
                                onMonthYearSelected(month, year)
                                isPickerVisible = false
                            }
                        )
                    }
                }

                var monthlyLimitText by remember { mutableStateOf("") }
                LaunchedEffect(budgetMonthlyLimitState) {
                    val modelAmount = budgetMonthlyLimitState
                    val textAmount = monthlyLimitText.toDoubleOrNull() ?: 0.0
                    if (modelAmount != textAmount) {
                        monthlyLimitText = if (modelAmount > 0) {
                            if (modelAmount == modelAmount.toLong().toDouble())
                                modelAmount.toLong().toString()
                            else modelAmount.toString()
                        } else ""
                    }
                }
                BudgetFormCard(
                    icon = Icons.Default.Payments,
                    title = stringResource(R.string.monthly_limit)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
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
                            onMonthlyLimitChanged(
                                sanitized.toDoubleOrNull() ?: 0.0
                            )
                        },
                        keyboardType = KeyboardType.Decimal
                    )
                }

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
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
private fun BudgetFormCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleEmphasis: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = BudgetFormCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (subtitleEmphasis)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (subtitleEmphasis) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            content()
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedMonth = index + 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
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
    categories: List<Category>,
    onCategoryChanged: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.find { it.id == selectedCategory }?.name
        ?: stringResource(R.string.select_category)

    Box(modifier = Modifier.fillMaxWidth()) {
        BudgetFormCard(
            icon = Icons.Default.Label,
            title = stringResource(R.string.category_label),
            subtitle = selectedCategoryName,
            subtitleEmphasis = selectedCategory != 0L,
            onClick = { expanded = true },
            trailingIcon = Icons.Default.ExpandMore
        )
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

@Composable
fun BudgetCategoryDropdown(
    selectedCategory: Long,
    categoryViewModel: CategoryViewModel,
    onCategoryChanged: (Long) -> Unit
) {
    val categories = categoryViewModel.getCategoriesByType(TransactionType.Gasto).collectAsState(initial = emptyList()).value
    BudgetCategoryCard(
        selectedCategory = selectedCategory,
        categories = categories,
        onCategoryChanged = onCategoryChanged
    )
}

@Preview(showBackground = true)
@Composable
fun AddEditDetailBudgetViewPreview() {
    val sampleCategories = listOf(
        Category(1L, "Alimentos", TransactionType.Gasto, "restaurant"),
        Category(2L, "Transporte", TransactionType.Gasto, "directions_bus"),
        Category(3L, "Ocio", TransactionType.Gasto, "movie")
    )

    EasyExpenseControlTheme {
        AddEditDetailBudgetContent(
            id = 0L,
            isLoading = false,
            displayDate = "Marzo 2024",
            budgetCategoryState = 1L,
            budgetMonthlyLimitState = 500.0,
            budgetMonthState = "03",
            budgetYearState = 2024,
            categories = sampleCategories,
            onBackClick = {},
            onCategoryChanged = {},
            onMonthYearSelected = { _, _ -> },
            onMonthlyLimitChanged = {},
            onSaveClick = {}
        )
    }
}
