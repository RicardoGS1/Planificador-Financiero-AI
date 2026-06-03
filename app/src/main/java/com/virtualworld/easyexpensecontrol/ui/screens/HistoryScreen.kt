package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import com.virtualworld.easyexpensecontrol.core.util.convertTimestampToString
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.EmbeddedControlBackground
import com.virtualworld.easyexpensecontrol.ui.theme.EmbeddedControlBorder
import com.virtualworld.easyexpensecontrol.ui.components.AccountFilterDropdown
import com.virtualworld.easyexpensecontrol.ui.components.ALL_ACCOUNTS_FILTER_ID
import com.virtualworld.easyexpensecontrol.ui.components.filterTransactionsByAccount
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val HistoryFiltersCardShape = RoundedCornerShape(20.dp)

@Composable
fun HistoryScreen(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    budgetViewModel: BudgetViewModel,
    accountViewModel: AccountViewModel,
    onPlaySound: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var sortAscending by remember { mutableStateOf(false) }
    var selectedAccountFilter by remember { mutableStateOf(ALL_ACCOUNTS_FILTER_ID) }
    val scope = rememberCoroutineScope()
    val accounts by accountViewModel.visibleAccounts.collectAsState(initial = emptyList())
    val allAccounts by accountViewModel.accounts.collectAsState(initial = emptyList())

    LaunchedEffect(accounts, selectedAccountFilter) {
        if (selectedAccountFilter != ALL_ACCOUNTS_FILTER_ID &&
            accounts.none { it.id == selectedAccountFilter }
        ) {
            selectedAccountFilter = ALL_ACCOUNTS_FILTER_ID
        }
    }

    Scaffold(
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        content = { paddingValues ->
            val transactionList = transactionViewModel.getAllTransactions
                .collectAsState(initial = emptyList())
            val categories = categoryViewModel.getAllCategories
                .collectAsState(initial = emptyList())

            val categoryNameMap = remember(categories.value) {
                categories.value.associate { it.id to it.name }
            }
            val accountNameMap = remember(allAccounts) {
                allAccounts.associate { it.id to it.name }
            }

            val filteredTransactions = remember(transactionList.value, selectedAccountFilter) {
                filterTransactionsByAccount(transactionList.value, selectedAccountFilter)
            }

            val sortedTransactions = remember(
                filteredTransactions,
                sortOption,
                sortAscending,
                categoryNameMap
            ) {
                val list = filteredTransactions
                val base = when (sortOption) {
                    SortOption.DATE -> list.sortedBy { it.date }
                    SortOption.AMOUNT -> list.sortedBy { it.amount }
                    SortOption.TYPE -> list.sortedBy { it.type.ordinal }
                    SortOption.CATEGORY -> list.sortedBy {
                        (categoryNameMap[it.category] ?: "").lowercase(Locale.getDefault())
                    }
                }
                if (sortAscending) base else base.reversed()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    ScreenHeader(title = stringResource(R.string.screen_transactions), showBackArrow = false)
                }
                item {
                    TransactionFiltersCard(
                        accounts = accounts,
                        selectedAccountFilter = selectedAccountFilter,
                        onAccountSelected = { selectedAccountFilter = it },
                        sortOption = sortOption,
                        ascending = sortAscending,
                        onSortOptionChange = { sortOption = it },
                        onToggleDirection = { sortAscending = !sortAscending },
                        showSortControls = filteredTransactions.isNotEmpty()
                    )
                }
                if (filteredTransactions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_transactions),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        )
                    }
                }
                items(sortedTransactions, key = { transaction -> transaction.id }) { transaction ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                transactionToDelete = transaction
                                showDialog = true
                                false
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        modifier = Modifier.animateContentSize(),
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                colorResource(R.color.red_transaction),
                                label = "dismiss_background"
                            )

                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete),
                                        tint = colorResource(R.color.blue_white)
                                    )
                                }
                            }
                        },
                        enableDismissFromEndToStart = true,
                        enableDismissFromStartToEnd = false,
                        content = {
                            TransactionItem(
                                transaction = transaction,
                                categoryViewModel = categoryViewModel,
                                accountName = accountNameMap[transaction.accountId]
                            ) {
                                val id = transaction.id
                                navController.navigate(Screen.AddEditTransactionScreen.route + "/$id")
                            }
                        }
                    )
                }
            }
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = {
                Text(
                    stringResource(R.string.delete_transaction_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val posibleBudget =
                                checkIfExistBudgetAssociatedWithTransaction(
                                    budgetViewModel,
                                    transactionToDelete
                                )
                            if (posibleBudget != null) {
                                val amountOfTransactionToDelete = transactionToDelete?.amount

                                val currentExpenditureAfterDeleteTransaction =
                                    posibleBudget.currentExpenditure - (amountOfTransactionToDelete ?: 0.0)

                                val budget = Budget(
                                    posibleBudget.id,
                                    posibleBudget.category,
                                    posibleBudget.monthlyLimit,
                                    currentExpenditureAfterDeleteTransaction,
                                    posibleBudget.month,
                                    posibleBudget.year
                                )

                                budgetViewModel.updateBudget(budget)
                            }
                        }

                        transactionToDelete?.let { transactionViewModel.deleteTransactionAndCheckCategory(it) }
                        onPlaySound(R.raw.delete_sound)
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.accept), color = colorResource(R.color.red_transaction))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

suspend fun checkIfExistBudgetAssociatedWithTransaction(
    budgetViewModel: BudgetViewModel,
    transactionToDelete: Transaction?
): Budget? {
    val instant = transactionToDelete?.date?.let {
        Instant.fromEpochMilliseconds(it)
    }

    val localDateTime =
        instant?.toLocalDateTime(TimeZone.currentSystemDefault())

    val month: Int = localDateTime?.monthNumber ?: 0
    val monthFormatted = month.toString().padStart(2, '0')
    val year: Int = localDateTime?.year ?: 0

    val posibleBudget = transactionToDelete?.category?.let {
        budgetViewModel
            .getBudgetForCategoryMonthAndYear(
                it,
                monthFormatted,
                year
            ).first()
    }

    return posibleBudget
}

private val IncomeGreen = Color(0xFF4CAF50)
private val ExpenseRed = Color(0xFFE33936)

@Composable
fun TransactionItem(
    transaction: Transaction,
    categoryViewModel: CategoryViewModel,
    accountName: String? = null,
    onClick: () -> Unit
) {
    val isIngreso = transaction.type == TransactionType.Ingreso
    val categoryFlow = categoryViewModel.getCategoryById(transaction.category)
    val category = categoryFlow.collectAsState(initial = Category(0L, "", TransactionType.Ingreso)).value
    val displayIcon = if (isIngreso) Icons.Default.ArrowDownward else CategoryIcons.getIcon(category.iconName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .background(AccentBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { stringResource(R.string.no_description) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = buildString {
                        append(category.name.ifEmpty { stringResource(R.string.no_category) })
                        if (!accountName.isNullOrBlank()) {
                            append(" · ")
                            append(accountName)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = convertTimestampToString(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = CurrencyFormatter.formatSigned(
                    LocalContext.current,
                    transaction.amount,
                    isIngreso
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIngreso) IncomeGreen else ExpenseRed
            )
        }
    }
}

enum class SortOption { DATE, AMOUNT, TYPE, CATEGORY }

private fun sortOptionLabelRes(option: SortOption): Int = when (option) {
    SortOption.DATE -> R.string.sort_by_date
    SortOption.AMOUNT -> R.string.sort_by_amount
    SortOption.TYPE -> R.string.sort_by_type
    SortOption.CATEGORY -> R.string.sort_by_category
}

@Composable
private fun TransactionFiltersCard(
    accounts: List<Account>,
    selectedAccountFilter: Long,
    onAccountSelected: (Long) -> Unit,
    sortOption: SortOption,
    ascending: Boolean,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleDirection: () -> Unit,
    showSortControls: Boolean,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            AccentBlue,
            MaterialTheme.colorScheme.primary
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = HistoryFiltersCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountFilterDropdown(
                accounts = accounts,
                selectedAccountId = selectedAccountFilter,
                onAccountSelected = onAccountSelected,
                label = stringResource(R.string.filter_by_account),
                embedded = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (showSortControls) {
                SortControls(
                    sortOption = sortOption,
                    ascending = ascending,
                    onSortOptionChange = onSortOptionChange,
                    onToggleDirection = onToggleDirection,
                    embedded = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SortControls(
    sortOption: SortOption,
    ascending: Boolean,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val chipColor = if (embedded) {
        EmbeddedControlBackground
    } else {
        MaterialTheme.colorScheme.surface
    }
    val chipBorder = if (embedded) {
        BorderStroke(1.dp, EmbeddedControlBorder)
    } else {
        null
    }
    val chipElevation = if (embedded) 0.dp else 2.dp
    val textColor = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = if (embedded) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.then(
            if (embedded) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = chipColor,
                border = chipBorder,
                tonalElevation = chipElevation,
                shadowElevation = chipElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sort_by_label) +
                            ": " +
                            stringResource(sortOptionLabelRes(sortOption)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_sort_options),
                        tint = iconTint
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sortOptionLabelRes(option))) },
                        onClick = {
                            onSortOptionChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            onClick = onToggleDirection,
            shape = RoundedCornerShape(12.dp),
            color = chipColor,
            border = chipBorder,
            tonalElevation = chipElevation,
            shadowElevation = chipElevation,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.cd_sort_invert),
                    tint = if (embedded) Color.White else AccentBlue
                )
            }
        }
    }
}
