package com.virtualworld.easyexpensecontrol.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.ads.InterstitialAdHelper
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import com.virtualworld.easyexpensecontrol.data.local.BudgetListVisibilityRepository
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

private object BudgetScreenAnimationSession {
    var hasPlayedEntryAnimation: Boolean = false
}

private val BudgetCardShape = RoundedCornerShape(20.dp)
private val BudgetProgressGreen = Color(0xFF4CAF50)
private val BudgetProgressOrange = Color(0xFFFFA726)
private val BudgetProgressRed = Color(0xFFE33936)

private fun isTransactionInMonth(timestamp: Long, year: Int, month: Int): Boolean {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) + 1 == month
}

@Composable
fun BudgetScreen(
    navController: NavHostController,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    transactionViewModel: TransactionViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? Activity ?: return@LaunchedEffect
        InterstitialAdHelper.showOnBudgetIfEnabled(activity)
    }

    val currentCalendar = Calendar.getInstance()
    val currentMonthNum = currentCalendar.get(Calendar.MONTH) + 1
    val currentMonth = currentMonthNum.toString().padStart(2, '0')
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val monthNames = stringArrayResource(R.array.month_names_full)
    val currentMonthLabel = monthNames.getOrElse(currentCalendar.get(Calendar.MONTH)) { currentMonth }

    LaunchedEffect(currentMonth, currentYear) {
        budgetViewModel.carryOverBudgetsIfNeeded(currentMonth, currentYear)
    }

    val expenseCategories by categoryViewModel
        .getCategoriesByType(TransactionType.Gasto)
        .collectAsState(initial = emptyList())

    val budgets by budgetViewModel.getAllBudgets.collectAsState(initial = emptyList())
    val currentMonthBudgets = remember(budgets, currentMonth, currentYear) {
        budgets.filter { it.month.padStart(2, '0') == currentMonth && it.year == currentYear }
    }

    val visibilityRepository: BudgetListVisibilityRepository = koinInject()
    val hiddenCategoryIds by visibilityRepository.hiddenCategoryIds.collectAsState(initial = emptySet())

    val allTransactions by transactionViewModel.getAllTransactions.collectAsState(initial = emptyList())

    val categorySpentMap = remember(allTransactions, expenseCategories, currentYear, currentMonthNum) {
        expenseCategories.associate { category ->
            category.id to allTransactions
                .filter { transaction ->
                    transaction.type == TransactionType.Gasto &&
                            transaction.category == category.id &&
                            isTransactionInMonth(transaction.date, currentYear, currentMonthNum)
                }
                .sumOf { it.amount }
        }
    }

    val visibleCategoryIds = remember(expenseCategories, hiddenCategoryIds) {
        expenseCategories.filter { it.id !in hiddenCategoryIds }.map { it.id }.toSet()
    }

    val budgetedCategoryIds = remember(currentMonthBudgets, visibleCategoryIds) {
        currentMonthBudgets
            .filter { it.category in visibleCategoryIds }
            .map { it.category }
            .toSet()
    }

    val totalLimit = remember(currentMonthBudgets, visibleCategoryIds) {
        currentMonthBudgets
            .filter { it.category in visibleCategoryIds }
            .sumOf { it.monthlyLimit }
    }

    val totalSpent = remember(categorySpentMap, budgetedCategoryIds) {
        categorySpentMap
            .filter { (categoryId, _) -> categoryId in budgetedCategoryIds }
            .values
            .sum()
    }

    val scope = rememberCoroutineScope()

    BudgetScreenContent(
        navController = navController,
        expenseCategories = expenseCategories,
        currentMonthBudgets = currentMonthBudgets,
        hiddenCategoryIds = hiddenCategoryIds,
        totalSpent = totalSpent,
        totalLimit = totalLimit,
        categorySpentMap = categorySpentMap,
        monthLabel = currentMonthLabel,
        year = currentYear,
        onBudgetClick = { category, budget ->
            if (budget != null) {
                navController.navigate(Screen.AddEditBudgetScreen.route + "/${budget.id}")
            } else {
                budgetViewModel.onBudgetCategoryChanged(category.id)
                budgetViewModel.onBudgetMonthChanged(currentMonth)
                budgetViewModel.onBudgetYearChanged(currentYear)
                navController.navigate(Screen.AddEditBudgetScreen.route + "/0L")
            }
        },
        onHistoryClick = { navController.navigate(Screen.BudgetHistoryScreen.route) },
        onApplyVisibility = { ids ->
            scope.launch {
                visibilityRepository.setHiddenCategoryIds(ids)
            }
        }
    )
}

@Composable
fun BudgetScreenContent(
    navController: NavController,
    expenseCategories: List<Category>,
    currentMonthBudgets: List<Budget>,
    hiddenCategoryIds: Set<Long>,
    totalSpent: Double,
    totalLimit: Double,
    categorySpentMap: Map<Long, Double>,
    monthLabel: String,
    year: Int,
    onBudgetClick: (Category, Budget?) -> Unit,
    onHistoryClick: () -> Unit,
    onApplyVisibility: (Set<Long>) -> Unit,
    isAnimationEnabled: Boolean = true
) {
    var showVisibilityDialog by remember { mutableStateOf(false) }
    var draftHiddenIds by remember { mutableStateOf(emptySet<Long>()) }

    var animationTarget by remember {
        mutableFloatStateOf(
            if (BudgetScreenAnimationSession.hasPlayedEntryAnimation || !isAnimationEnabled) 1f else 0f
        )
    }
    val entryAnimationProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "budget_entry_animation"
    )

    LaunchedEffect(Unit) {
        if (!BudgetScreenAnimationSession.hasPlayedEntryAnimation) {
            animationTarget = 1f
            BudgetScreenAnimationSession.hasPlayedEntryAnimation = true
        }
    }

    val visibleExpenseCategories = remember(expenseCategories, hiddenCategoryIds) {
        expenseCategories.filter { it.id !in hiddenCategoryIds }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (expenseCategories.isEmpty()) {
            BudgetEmptyState(
                modifier = Modifier.padding(paddingValues),
                message = stringResource(R.string.budget_no_categories_message)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    ScreenHeader(
                        title = stringResource(R.string.screen_budget),
                        showBackArrow = false,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onHistoryClick) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = stringResource(R.string.cd_open_budget_history),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        draftHiddenIds = hiddenCategoryIds
                                        showVisibilityDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = stringResource(R.string.cd_budget_list_visibility),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    )
                }

                if (visibleExpenseCategories.isNotEmpty() && totalLimit > 0) {
                    item {
                        BudgetSummaryCard(
                            monthLabel = monthLabel,
                            year = year,
                            totalSpent = totalSpent,
                            totalLimit = totalLimit,
                            animationProgress = entryAnimationProgress,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.budget_categories_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (visibleExpenseCategories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.budget_list_all_hidden_message),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(visibleExpenseCategories, key = { category -> category.id }) { category ->
                        val categoryBudget = currentMonthBudgets.find { it.category == category.id }
                        val spent = categorySpentMap[category.id] ?: 0.0
                        ExpenseBudgetItem(
                            category = category,
                            budget = categoryBudget,
                            spent = spent,
                            animationProgress = entryAnimationProgress,
                            onActionClick = { onBudgetClick(category, categoryBudget) }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        if (showVisibilityDialog) {
            AlertDialog(
                onDismissRequest = { showVisibilityDialog = false },
                title = { Text(stringResource(R.string.budget_list_visibility_title)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { draftHiddenIds = emptySet() }) {
                                Text(stringResource(R.string.budget_list_visibility_show_all))
                            }
                        }
                        Text(
                            text = stringResource(R.string.budget_list_visibility_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        expenseCategories.forEach { category ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = category.id !in draftHiddenIds,
                                    onCheckedChange = { visible ->
                                        draftHiddenIds =
                                            if (visible) draftHiddenIds - category.id else draftHiddenIds + category.id
                                    }
                                )
                                Text(
                                    text = category.name.ifEmpty { stringResource(R.string.no_category) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onApplyVisibility(draftHiddenIds)
                            showVisibilityDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.budget_list_visibility_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVisibilityDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun BudgetEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = stringResource(R.string.screen_budget), showBackArrow = false)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    monthLabel: String,
    year: Int,
    totalSpent: Double,
    totalLimit: Double,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeProgress = animationProgress.coerceIn(0f, 1f)
    val displayedSpent = totalSpent * safeProgress
    val usageFraction = if (totalLimit > 0) (displayedSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val usagePercent = (usageFraction * 100).toInt()
    val isOverBudget = displayedSpent > totalLimit
    val progressColor = when {
        isOverBudget -> BudgetProgressRed
        usageFraction > 0.8f -> BudgetProgressOrange
        else -> BudgetProgressGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = BudgetCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "$monthLabel $year",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.budget_summary_spent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = CurrencyFormatter.format(context, displayedSpent),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.budget_summary_limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = CurrencyFormatter.format(context, totalLimit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(usageFraction.coerceAtLeast(0.02f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.budget_usage_percent, usagePercent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun ExpenseBudgetItem(
    category: Category,
    budget: Budget?,
    spent: Double,
    animationProgress: Float = 1f,
    onActionClick: () -> Unit,
    isActionEnabled: Boolean = true,
    showActionButton: Boolean = true
) {
    val context = LocalContext.current
    val currencySymbol = CurrencyFormatter.symbol(context)
    val hasBudget = budget != null
    val limit = budget?.monthlyLimit ?: 0.0
    val safeProgress = animationProgress.coerceIn(0f, 1f)
    val displayedSpent = if (hasBudget) spent * safeProgress else spent
    val remaining = limit - displayedSpent
    val isOverBudget = hasBudget && remaining < 0
    val spentFraction = if (hasBudget && limit > 0.0) (displayedSpent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val statusLabel = when {
        !hasBudget -> stringResource(R.string.budget_status_no_budget)
        isOverBudget -> stringResource(R.string.budget_status_over)
        spentFraction > 0.8f -> stringResource(R.string.budget_status_warning)
        else -> stringResource(R.string.budget_status_ok)
    }
    val statusColor = when {
        !hasBudget -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverBudget -> BudgetProgressRed
        spentFraction > 0.8f -> BudgetProgressOrange
        else -> BudgetProgressGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(enabled = isActionEnabled) { onActionClick() },
        shape = BudgetCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BudgetCategoryIcon(
                    icon = CategoryIcons.getIcon(category.iconName),
                    progress = spentFraction,
                    hasBudget = hasBudget,
                    isOverBudget = isOverBudget
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name.ifEmpty { stringResource(R.string.no_category) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    if (hasBudget) {
                        Text(
                            text = stringResource(
                                R.string.budget_spent_limit,
                                displayedSpent,
                                currencySymbol,
                                limit
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.budget_spent_only, displayedSpent, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BudgetStatusChip(label = statusLabel, color = statusColor)
            }

            if (hasBudget) {
                Spacer(Modifier.height(12.dp))
                BudgetProgressBar(
                    spentFraction = spentFraction,
                    isOverBudget = isOverBudget
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val remainingVisibleState = remember {
                        MutableTransitionState(!isOverBudget)
                    }
                    LaunchedEffect(isOverBudget) {
                        remainingVisibleState.targetState = !isOverBudget
                    }
                    val canShowAttention = isOverBudget &&
                        !remainingVisibleState.currentState &&
                        remainingVisibleState.isIdle

                    AnimatedVisibility(
                        visible = canShowAttention,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = stringResource(R.string.budget_attention),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BudgetProgressRed
                        )
                    }
                    AnimatedVisibility(
                        visibleState = remainingVisibleState,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = stringResource(R.string.budget_remaining, remaining, currencySymbol),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = BudgetProgressGreen
                        )
                    }
                }
            }

            if (showActionButton) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onActionClick() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasBudget) stringResource(R.string.increase_budget)
                        else stringResource(R.string.set_budget),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryIcon(
    icon: ImageVector,
    progress: Float,
    hasBudget: Boolean,
    isOverBudget: Boolean
) {
    val ringColor = when {
        !hasBudget -> Color.Transparent
        isOverBudget -> BudgetProgressRed
        progress > 0.8f -> BudgetProgressOrange
        else -> BudgetProgressGreen
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hasBudget) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = (size.minDimension - diameter) / 2f
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AccentBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun BudgetStatusChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BudgetProgressBar(
    spentFraction: Float,
    isOverBudget: Boolean
) {
    val clampedFraction = spentFraction.coerceIn(0f, 1f)
    val barColor = when {
        isOverBudget -> BudgetProgressRed
        clampedFraction > 0.8f -> BudgetProgressOrange
        else -> BudgetProgressGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (clampedFraction > 0f) clampedFraction else 0.02f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(barColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetScreenPreview() {
    val sampleCategories = listOf(
        Category(1, "Alimentos", TransactionType.Gasto, "restaurant"),
        Category(2, "Transporte", TransactionType.Gasto, "directions_bus"),
        Category(3, "Ocio", TransactionType.Gasto, "movie"),
        Category(4, "Vivienda", TransactionType.Gasto, "home")
    )
    val sampleBudgets = listOf(
        Budget(1, 1, 500.0, 0.0, "10", 2023),
        Budget(2, 2, 200.0, 0.0, "10", 2023)
    )
    val categorySpentMap = mapOf(
        1L to 350.0,
        2L to 250.0,
        3L to 50.0,
        4L to 800.0
    )

    EasyExpenseControlTheme {
        BudgetScreenContent(
            navController = rememberNavController(),
            expenseCategories = sampleCategories,
            currentMonthBudgets = sampleBudgets,
            hiddenCategoryIds = emptySet(),
            totalSpent = 1450.0,
            totalLimit = 700.0,
            categorySpentMap = categorySpentMap,
            monthLabel = "Octubre",
            year = 2023,
            onBudgetClick = { _, _ -> },
            onHistoryClick = {},
            onApplyVisibility = {},
            isAnimationEnabled = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetSummaryCardPreview() {
    EasyExpenseControlTheme {
        BudgetSummaryCard(
            monthLabel = "Octubre",
            year = 2023,
            totalSpent = 350.0,
            totalLimit = 500.0,
            animationProgress = 1f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseBudgetItemPreview() {
    val sampleCategory = Category(1, "Alimentos", TransactionType.Gasto, "restaurant")
    val sampleBudget = Budget(1, 1, 500.0, 0.0, "10", 2023)
    EasyExpenseControlTheme {
        ExpenseBudgetItem(
            category = sampleCategory,
            budget = sampleBudget,
            spent = 350.0,
            onActionClick = {}
        )
    }
}
