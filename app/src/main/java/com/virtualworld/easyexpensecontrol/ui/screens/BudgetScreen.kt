package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.FilterList
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.ads.InterstitialAdHelper
import com.virtualworld.easyexpensecontrol.data.local.BudgetListVisibilityRepository
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

private object BudgetScreenAnimationSession {
    var hasPlayedEntryAnimation: Boolean = false
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
        InterstitialAdHelper.show(activity)
    }

    val currentCalendar = Calendar.getInstance()
    val currentMonth = (currentCalendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val expenseCategories = categoryViewModel
        .getCategoriesByType(TransactionType.Gasto)
        .collectAsState(initial = emptyList())
        .value
    val budgets = budgetViewModel.getAllBudgets.collectAsState(initial = emptyList()).value
    val currentMonthBudgets = budgets.filter { it.month.padStart(2, '0') == currentMonth && it.year == currentYear }

    val visibilityRepository: BudgetListVisibilityRepository = koinInject()
    val hiddenCategoryIds by visibilityRepository.hiddenCategoryIds.collectAsState(initial = emptySet())
    var showVisibilityDialog by remember { mutableStateOf(false) }
    var draftHiddenIds by remember { mutableStateOf(emptySet<Long>()) }
    val scope = rememberCoroutineScope()
    var animationTarget by remember {
        mutableFloatStateOf(
            if (BudgetScreenAnimationSession.hasPlayedEntryAnimation) 1f else 0f
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
                    ScreenHeader(
                        title = stringResource(R.string.screen_budget),
                        showBackArrow = false,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { navController.navigate(Screen.BudgetHistoryScreen.route) }
                                ) {
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
                    Spacer(Modifier.height(8.dp))
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
                        val spent = transactionViewModel
                            .getTransactionsByCategoryAndDate(category.id, currentYear, currentMonth)
                            .collectAsState(initial = emptyList())
                            .value
                            .sumOf { it.amount }
                        ExpenseBudgetItem(
                            category = category,
                            budget = categoryBudget,
                            spent = spent,
                            animationProgress = entryAnimationProgress,
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

        if (showVisibilityDialog) {
            AlertDialog(
                onDismissRequest = { showVisibilityDialog = false },
                title = { Text(stringResource(R.string.budget_list_visibility_title)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
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
                            scope.launch {
                                visibilityRepository.setHiddenCategoryIds(draftHiddenIds)
                                showVisibilityDialog = false
                            }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = isActionEnabled) { onActionClick() },
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
                    imageVector = CategoryIcons.getIcon(category.iconName),
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
                        val remainingVisibleState = remember {
                            MutableTransitionState(!isOverBudget)
                        }
                        LaunchedEffect(isOverBudget) {
                            remainingVisibleState.targetState = !isOverBudget
                        }
                        val canShowAttention = isOverBudget &&
                            !remainingVisibleState.currentState &&
                            remainingVisibleState.isIdle

                        Text(
                            text = stringResource(
                                R.string.budget_spent_limit,
                                displayedSpent,
                                currencySymbol,
                                limit
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) BudgetProgressRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedVisibility(
                            visible = canShowAttention,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = stringResource(R.string.budget_attention),
                                style = MaterialTheme.typography.labelLarge,
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
                                text = stringResource(
                                    R.string.budget_remaining,
                                    remaining,
                                    currencySymbol
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = BudgetProgressGreen
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BudgetSplitBar(
                        spentFraction = spentFraction,
                        isOverBudget = isOverBudget
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (showActionButton) {
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
}

@Composable
private fun BudgetSplitBar(
    spentFraction: Float,
    isOverBudget: Boolean
) {
    val clampedSpentFraction = spentFraction.coerceIn(0f, 1f)
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
                    .fillMaxWidth(clampedSpentFraction)
                    .fillMaxSize()
                    .background(BudgetProgressRed)
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(clampedSpentFraction.coerceAtLeast(0.0001f))
                        .fillMaxSize()
                        .background(BudgetProgressRed)
                )
                Box(
                    modifier = Modifier
                        .weight((1f - clampedSpentFraction).coerceIn(0f, 1f).coerceAtLeast(0.0001f))
                        .fillMaxSize()
                        .background(BudgetProgressGreen)
                )
            }
        }
    }
}
