package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.Settings
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeDayPeriods
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeMonthPeriods
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeWeekPeriods
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CoachmarkArrowDirection
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.components.TutorialCoachmarkOverlay
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.virtualworld.easyexpensecontrol.data.local.OnboardingTutorialRepository
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel
import kotlin.random.Random
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.virtualworld.easyexpensecontrol.ads.AiRewardedAdHelper

private enum class ChartPeriod { DAY, WEEK, MONTH }

private enum class DashboardTutorialStep {
    None,
    DefaultAccount,
    TotalBalance,
    AddTransaction
}

private const val MAX_ULTIMAS_ENTRADAS = 15
private val DashboardCardShape = RoundedCornerShape(20.dp)
private val DashboardChipShape = RoundedCornerShape(12.dp)
private var hasAnimatedDashboardBarsInSession = false
private var hasAnimatedBalanceInSession = false

@Composable
fun DashboardScreen(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    accountViewModel: AccountViewModel
) {
    val listaTransacciones by transactionViewModel.getAllTransactions.collectAsState(initial = emptyList())
    val pendingAutoGenerated by transactionViewModel.pendingAutoGeneratedTransactions.collectAsState()
    val categories by categoryViewModel.getAllCategories.collectAsState(initial = emptyList())
    val accounts by accountViewModel.visibleAccounts.collectAsState(initial = emptyList())
    val allAccounts by accountViewModel.accounts.collectAsState(initial = emptyList())
    val tutorialRepository: OnboardingTutorialRepository = koinInject()
    val accountTipSeen by tutorialRepository.defaultAccountTipSeen.collectAsState(initial = true)
    val currencyTipSeen by tutorialRepository.currencyTipSeen.collectAsState(initial = true)
    val addTransactionTipSeen by tutorialRepository.addTransactionTipSeen.collectAsState(initial = true)
    var preferAddAccountTab by remember { mutableStateOf<Boolean?>(null) }
    var showRecurringAutoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pendingAutoGenerated) {
        if (pendingAutoGenerated.isNotEmpty()) {
            showRecurringAutoDialog = true
        }
    }

    if (showRecurringAutoDialog && pendingAutoGenerated.isNotEmpty()) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = {
                showRecurringAutoDialog = false
                transactionViewModel.clearPendingAutoGenerated()
            },
            title = { Text(stringResource(R.string.recurring_auto_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.recurring_auto_dialog_message,
                            pendingAutoGenerated.size
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pendingAutoGenerated.forEach { transaction ->
                        val isIngreso = transaction.type == TransactionType.Ingreso
                        val amountText = CurrencyFormatter.formatSigned(
                            context,
                            transaction.amount,
                            isIngreso
                        )
                        val description = transaction.description.ifEmpty {
                            stringResource(R.string.no_description)
                        }
                        Text(
                            text = stringResource(
                                R.string.recurring_auto_dialog_item,
                                description,
                                amountText
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRecurringAutoDialog = false
                        transactionViewModel.clearPendingAutoGenerated()
                    }
                ) {
                    Text(stringResource(R.string.recurring_dialog_ok))
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        preferAddAccountTab = accountViewModel.visibleAccounts.first().isEmpty()
    }

    DashboardScreen(
        navController = navController,
        transactions = listaTransacciones,
        categories = categories,
        accounts = accounts,
        allAccounts = allAccounts,
        preferAddAccountTab = preferAddAccountTab,
        accountTipSeen = accountTipSeen,
        currencyTipSeen = currencyTipSeen,
        addTransactionTipSeen = addTransactionTipSeen,
        onDefaultAccountTipDismissed = { tutorialRepository.markDefaultAccountTipSeen() },
        onCurrencyTipDismissed = { tutorialRepository.markCurrencyTipSeen() },
        onAddTransactionTipDismissed = { tutorialRepository.markAddTransactionTipSeen() },
        onAddAccount = { name, onError, onSuccess ->
            accountViewModel.addAccount(name, onError, onSuccess)
        },
        onSettingsClick = { navController.navigate(Screen.SettingsScreen.route) }
    )
}

@Composable
fun DashboardScreen(
    navController: NavController,
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    allAccounts: List<Account> = accounts,
    preferAddAccountTab: Boolean? = null,
    accountTipSeen: Boolean = true,
    currencyTipSeen: Boolean = true,
    addTransactionTipSeen: Boolean = true,
    onDefaultAccountTipDismissed: suspend () -> Unit = {},
    onCurrencyTipDismissed: suspend () -> Unit = {},
    onAddTransactionTipDismissed: suspend () -> Unit = {},
    onAddAccount: (name: String, onError: suspend (String) -> Unit, onSuccess: suspend (Long) -> Unit) -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var addAccountError by remember { mutableStateOf<String?>(null) }
    var hasAppliedInitialTab by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableStateOf(DashboardTutorialStep.None) }
    var defaultAccountBounds by remember { mutableStateOf<Rect?>(null) }
    var totalBalanceBounds by remember { mutableStateOf<Rect?>(null) }
    var fabBounds by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!AiRewardedAdHelper.hasSessionAccess()) {
            AiRewardedAdHelper.preload(context)
        }
    }

    val defaultAccountVisible = accounts.any { it.id == OnboardingTutorialRepository.DEFAULT_ACCOUNT_ID }

    LaunchedEffect(accountTipSeen, currencyTipSeen, addTransactionTipSeen, defaultAccountVisible) {
        tutorialStep = when {
            !accountTipSeen && defaultAccountVisible -> DashboardTutorialStep.DefaultAccount
            accountTipSeen && !currencyTipSeen -> DashboardTutorialStep.TotalBalance
            accountTipSeen && currencyTipSeen && !addTransactionTipSeen -> DashboardTutorialStep.AddTransaction
            else -> DashboardTutorialStep.None
        }
    }

    LaunchedEffect(tutorialStep, accounts, selectedTabIndex) {
        if (tutorialStep == DashboardTutorialStep.DefaultAccount) {
            val defaultIndex = accounts.indexOfFirst { it.id == OnboardingTutorialRepository.DEFAULT_ACCOUNT_ID }
            if (defaultIndex >= 0) {
                selectedTabIndex = defaultIndex + 1
            }
            return@LaunchedEffect
        }
        if (tutorialStep == DashboardTutorialStep.TotalBalance && selectedTabIndex == accounts.size + 1) {
            selectedTabIndex = 0
        }
    }

    val isAddTab = selectedTabIndex == accounts.size + 1

    LaunchedEffect(preferAddAccountTab, accounts.size) {
        if (!hasAppliedInitialTab) {
            if (preferAddAccountTab == null) return@LaunchedEffect
            hasAppliedInitialTab = true
            if (preferAddAccountTab) {
                selectedTabIndex = accounts.size + 1
            }
            return@LaunchedEffect
        }
        if (selectedTabIndex > 0 && selectedTabIndex <= accounts.size) return@LaunchedEffect
        if (selectedTabIndex == accounts.size + 1) return@LaunchedEffect
        if (selectedTabIndex != 0) {
            selectedTabIndex = 0
        }
    }

    val filteredTransactions = remember(transactions, selectedTabIndex, accounts) {
        when {
            selectedTabIndex == 0 -> transactions
            isAddTab -> transactions
            else -> {
                val accountId = accounts.getOrNull(selectedTabIndex - 1)?.id
                if (accountId != null) transactions.filter { it.accountId == accountId }
                else transactions
            }
        }
    }

    val accountNameMap = remember(allAccounts) { allAccounts.associate { it.id to it.name } }
    val balanceLabel = when {
        selectedTabIndex == 0 -> stringResource(R.string.total_balance)
        isAddTab -> stringResource(R.string.total_balance)
        else -> accounts.getOrNull(selectedTabIndex - 1)?.name ?: stringResource(R.string.total_balance)
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddAccountDialog = false
                newAccountName = ""
                addAccountError = null
            },
            title = { Text(stringResource(R.string.add_account_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.add_account_examples),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = {
                            newAccountName = it
                            addAccountError = null
                        },
                        label = { Text(stringResource(R.string.account_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    addAccountError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAccountName.isBlank()) {
                            addAccountError = context.getString(R.string.err_account_name_empty)
                            return@TextButton
                        }
                        onAddAccount(
                            newAccountName,
                            { error ->
                                addAccountError = error.ifBlank {
                                    context.getString(R.string.error_unknown)
                                }
                            },
                            { newId ->
                                showAddAccountDialog = false
                                newAccountName = ""
                                addAccountError = null
                                selectedTabIndex = accounts.size + 1
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.add_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddAccountDialog = false
                    newAccountName = ""
                    addAccountError = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                CurvedBottomBar(
                    navController = navController,
                    fabModifier = Modifier.onGloballyPositioned { coordinates ->
                        fabBounds = coordinates.boundsInWindow()
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
        var income = 0.0
        var expenses = 0.0
        filteredTransactions.forEach { t ->
            if (t.type == TransactionType.Ingreso) income += t.amount
            else if (t.type == TransactionType.Gasto) expenses += t.amount
        }
        val balance = income - expenses

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = stringResource(R.string.screen_financial_planner),
                showBackArrow = false,
                trailingContent = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )

            DashboardAccountTabs(
                accounts = accounts,
                selectedTabIndex = selectedTabIndex,
                isAddTab = isAddTab,
                onSelectTab = { selectedTabIndex = it },
                defaultAccountChipModifier = Modifier.onGloballyPositioned { coordinates ->
                    defaultAccountBounds = coordinates.boundsInWindow()
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isAddTab) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.add_account_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showAddAccountDialog = true }) {
                            Text(stringResource(R.string.add_account_title))
                        }
                    }
                }
            } else {
                TotalBalanceSection(
                    balance = balance,
                    label = balanceLabel,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        totalBalanceBounds = coordinates.boundsInWindow()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PeriodChart(
                    transactions = filteredTransactions,
                    todayLabel = stringResource(R.string.day_today),
                    yesterdayLabel = stringResource(R.string.day_yesterday),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.last_entries),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LatestTransactionsList(
                    transactions = filteredTransactions,
                    categories = categories,
                    accountNameMap = if (selectedTabIndex == 0) accountNameMap else emptyMap(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
        }

        when (tutorialStep) {
            DashboardTutorialStep.DefaultAccount -> {
                defaultAccountBounds?.let { bounds ->
                    TutorialCoachmarkOverlay(
                        targetBounds = bounds,
                        message = stringResource(R.string.tutorial_default_account_message),
                        buttonText = stringResource(R.string.tutorial_got_it),
                        arrowDirection = CoachmarkArrowDirection.Up,
                        onDismiss = {
                            scope.launch { onDefaultAccountTipDismissed() }
                        }
                    )
                }
            }
            DashboardTutorialStep.TotalBalance -> {
                totalBalanceBounds?.let { bounds ->
                    TutorialCoachmarkOverlay(
                        targetBounds = bounds,
                        message = stringResource(R.string.tutorial_currency_message),
                        buttonText = stringResource(R.string.tutorial_got_it),
                        arrowDirection = CoachmarkArrowDirection.Up,
                        onDismiss = {
                            scope.launch { onCurrencyTipDismissed() }
                        }
                    )
                }
            }
            DashboardTutorialStep.AddTransaction -> {
                fabBounds?.let { bounds ->
                    TutorialCoachmarkOverlay(
                        targetBounds = bounds,
                        message = stringResource(R.string.tutorial_add_transaction_message),
                        buttonText = stringResource(R.string.tutorial_got_it),
                        arrowDirection = CoachmarkArrowDirection.Down,
                        onDismiss = {
                            scope.launch { onAddTransactionTipDismissed() }
                        }
                    )
                }
            }
            DashboardTutorialStep.None -> Unit
        }
    }
}

@Composable
private fun DashboardAccountTabs(
    accounts: List<Account>,
    selectedTabIndex: Int,
    isAddTab: Boolean,
    onSelectTab: (Int) -> Unit,
    defaultAccountChipModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DashboardTabChip(
            label = stringResource(R.string.tab_all_accounts),
            selected = selectedTabIndex == 0,
            onClick = { onSelectTab(0) }
        )
        accounts.forEachIndexed { index, account ->
            val chipModifier = if (account.id == OnboardingTutorialRepository.DEFAULT_ACCOUNT_ID) {
                defaultAccountChipModifier
            } else {
                Modifier
            }
            DashboardTabChip(
                label = account.name,
                selected = selectedTabIndex == index + 1,
                onClick = { onSelectTab(index + 1) },
                modifier = chipModifier
            )
        }
        DashboardTabChip(
            label = null,
            selected = isAddTab,
            onClick = { onSelectTab(accounts.size + 1) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_account_title),
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun DashboardTabChip(
    label: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(DashboardChipShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (label != null) 14.dp else 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TotalBalanceSection(
    balance: Double,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedBalance = remember { Animatable(0f) }
    val balanceLabel = label ?: stringResource(R.string.total_balance)
    val displayBalance = animatedBalance.value.toDouble()
    val accentColor = when {
        balance > 0 -> MaterialTheme.colorScheme.primary
        balance < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val currencySymbol = CurrencyFormatter.symbol(LocalContext.current)
    val amountText = "%.2f".format(Locale.getDefault(), abs(displayBalance))
    val signText = when {
        displayBalance > 0 -> "+"
        displayBalance < 0 -> "−"
        else -> null
    }
    val amountTextStyle = TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor
    )
    LaunchedEffect(balance) {
        if (!hasAnimatedBalanceInSession) {
            val startValue = if (balance == 0.0) 1000f else 0f
            animatedBalance.snapTo(startValue)
            animatedBalance.animateTo(
                targetValue = balance.toFloat(),
                animationSpec = tween(durationMillis = 1000)
            )
            hasAnimatedBalanceInSession = true
        } else {
            animatedBalance.snapTo(balance.toFloat())
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (signText != null) {
                    Text(
                        text = signText,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Text(
                    text = amountText,
                    style = amountTextStyle
                )
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PeriodChart(
    transactions: List<Transaction>,
    todayLabel: String,
    yesterdayLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.MONTH) }
    val monthNamesShort = stringArrayResource(R.array.month_names_short)

    val periods = when (selectedPeriod) {
        ChartPeriod.DAY -> getLastThreeDayPeriods(todayLabel, yesterdayLabel)
        ChartPeriod.WEEK -> getLastThreeWeekPeriods()
        ChartPeriod.MONTH -> getLastThreeMonthPeriods(monthNamesShort)
    }.reversed()

    val periodData = periods.map { period ->
        val income = transactions
            .filter { it.type == TransactionType.Ingreso && it.date >= period.startMs && it.date < period.endMs }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.type == TransactionType.Gasto && it.date >= period.startMs && it.date < period.endMs }
            .sumOf { it.amount }
        Triple(period.label, income, expense)
    }

    val maxVal = periodData.flatMap { listOf(it.second, it.third) }.maxOrNull() ?: 1.0
    val maxHeight = maxVal.coerceAtLeast(1.0)
    val barCount = periodData.size * 2
    val barProgresses = remember { mutableStateListOf<Float>() }

    LaunchedEffect(barCount) {
        barProgresses.clear()
        repeat(barCount) {
            barProgresses.add(if (hasAnimatedDashboardBarsInSession) 1f else 0f)
        }

        if (!hasAnimatedDashboardBarsInSession) {
            coroutineScope {
                repeat(barCount) { barIndex ->
                    launch {
                        val animatable = Animatable(0f)
                        val randomDuration = Random.nextInt(500, 1301)
                        animatable.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = randomDuration)
                        ) {
                            barProgresses[barIndex] = value
                        }
                    }
                }
            }
            hasAnimatedDashboardBarsInSession = true
        }
    }

    val dayLabel = stringResource(R.string.period_day)
    val weekLabel = stringResource(R.string.period_week)
    val monthLabel = stringResource(R.string.period_month)
    val incomeColor = colorResource(R.color.green_transaction)
    val expenseColor = colorResource(R.color.red_transaction)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val density = LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }

    Card(
        modifier = modifier,
        shape = DashboardCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = DashboardCardShape
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_income) + " / " + stringResource(R.string.label_expense),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (selectedPeriod) {
                            ChartPeriod.DAY -> dayLabel
                            ChartPeriod.WEEK -> weekLabel
                            ChartPeriod.MONTH -> monthLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                PeriodSelector(
                    selected = selectedPeriod,
                    onSelect = { selectedPeriod = it },
                    dayLabel = dayLabel,
                    weekLabel = weekLabel,
                    monthLabel = monthLabel
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val bottomPadding = 32f
                val chartHeight = size.height - bottomPadding
                val barGroupWidth = size.width / 3f
                val barWidth = barGroupWidth / 3.2f
                val gap = barWidth * 0.25f
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                val gridSteps = 3

                for (step in 0..gridSteps) {
                    val y = chartHeight * (1f - step.toFloat() / gridSteps)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                periodData.forEachIndexed { index, (label, ing, gas) ->
                    val groupLeft = index * barGroupWidth + barGroupWidth * 0.08f
                    val ingHeight = (ing / maxHeight).toFloat().coerceIn(0f, 1f) * chartHeight
                    val gasHeight = (gas / maxHeight).toFloat().coerceIn(0f, 1f) * chartHeight
                    val ingProgress = barProgresses.getOrNull(index * 2) ?: 1f
                    val gasProgress = barProgresses.getOrNull(index * 2 + 1) ?: 1f
                    val animatedIngHeight = ingHeight + (chartHeight - ingHeight) * (1f - ingProgress)
                    val animatedGasHeight = gasHeight + (chartHeight - gasHeight) * (1f - gasProgress)

                    val bar1Left = groupLeft + gap
                    val bar2Left = groupLeft + barWidth + gap * 1.5f
                    val actualBarWidth = barWidth - gap

                    drawRoundRect(
                        color = incomeColor,
                        topLeft = Offset(bar1Left, chartHeight - animatedIngHeight),
                        size = Size(actualBarWidth, animatedIngHeight.coerceAtLeast(6f)),
                        cornerRadius = cornerRadius
                    )
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = Offset(bar2Left, chartHeight - animatedGasHeight),
                        size = Size(actualBarWidth, animatedGasHeight.coerceAtLeast(6f)),
                        cornerRadius = cornerRadius
                    )

                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            textSize = axisLabelSizePx
                            color = labelColor
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            label,
                            groupLeft + barGroupWidth * 0.42f,
                            size.height - 6f,
                            paint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = incomeColor, label = stringResource(R.string.label_income))
                Spacer(modifier = Modifier.size(20.dp))
                ChartLegendItem(color = expenseColor, label = stringResource(R.string.label_expense))
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PeriodSelector(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
    dayLabel: String,
    weekLabel: String,
    monthLabel: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(3.dp)
    ) {
        listOf(
            ChartPeriod.DAY to dayLabel,
            ChartPeriod.WEEK to weekLabel,
            ChartPeriod.MONTH to monthLabel
        ).forEach { (period, label) ->
            val isSelected = period == selected
            Text(
                text = label,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(period) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun LatestTransactionsList(
    transactions: List<Transaction>,
    categoryViewModel: CategoryViewModel,
    modifier: Modifier = Modifier
) {
    val categories by categoryViewModel.getAllCategories.collectAsState(initial = emptyList())
    LatestTransactionsList(
        transactions = transactions,
        categories = categories,
        modifier = modifier
    )
}

@Composable
fun LatestTransactionsList(
    transactions: List<Transaction>,
    categories: List<Category>,
    accountNameMap: Map<Long, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val sorted = transactions
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
        .take(MAX_ULTIMAS_ENTRADAS)
    Card(
        modifier = modifier,
        shape = DashboardCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = DashboardCardShape
                )
        ) {
            if (sorted.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_recent_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(sorted, key = { _, t -> t.id }) { _, transaction ->
                        val category = categories.find { it.id == transaction.category }
                            ?: Category(0L, "", TransactionType.Ingreso)
                        LatestTransactionRow(
                            transaction = transaction,
                            category = category,
                            accountName = accountNameMap[transaction.accountId]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatestTransactionRow(
    transaction: Transaction,
    categoryViewModel: CategoryViewModel
) {
    val category by categoryViewModel.getCategoryById(transaction.category)
        .collectAsState(initial = Category(0L, "", TransactionType.Ingreso))
    LatestTransactionRow(
        transaction = transaction,
        category = category
    )
}

@Composable
fun LatestTransactionRow(
    transaction: Transaction,
    category: Category,
    accountName: String? = null
) {
    val isIngreso = transaction.type == TransactionType.Ingreso
    val displayIcon = if (isIngreso) Icons.Default.ArrowDownward else CategoryIcons.getIcon(category.iconName)
    val incomeColor = colorResource(R.color.green_transaction)
    val expenseColor = colorResource(R.color.red_transaction)
    val accentColor = if (isIngreso) incomeColor else expenseColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifEmpty { stringResource(R.string.no_description) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        val context = LocalContext.current
        Text(
            text = CurrencyFormatter.formatSigned(context, transaction.amount, isIngreso),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val sampleCategories = listOf(
        Category(1, "Alimentos", TransactionType.Gasto, "restaurant"),
        Category(2, "Sueldo", TransactionType.Ingreso, "payments"),
        Category(3, "Vivienda", TransactionType.Gasto, "home"),
        Category(4, "Transporte", TransactionType.Gasto, "directions_bus"),
        Category(5, "Ocio", TransactionType.Gasto, "movie")
    )

    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    val sampleTransactions = listOf(
        Transaction(1, TransactionType.Ingreso, 2500.0, 2, now, "Sueldo Mensual"),
        Transaction(2, TransactionType.Gasto, 45.50, 1, now, "Cena fuera"),
        Transaction(3, TransactionType.Gasto, 800.0, 3, now - dayMs, "Alquiler"),
        Transaction(4, TransactionType.Gasto, 20.0, 4, now - 2 * dayMs, "Bono Metro"),
        Transaction(5, TransactionType.Gasto, 15.0, 5, now - 3 * dayMs, "Cine"),
        Transaction(6, TransactionType.Gasto, 60.0, 1, now - 5 * dayMs, "Compra supermercado")
    )

    EasyExpenseControlTheme {
        DashboardScreen(
            navController = rememberNavController(),
            transactions = sampleTransactions,
            categories = sampleCategories,
            accounts = listOf(Account(1, "General")),
            onAddAccount = { _, _, _ -> },
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TotalBalanceSectionPreview() {
    EasyExpenseControlTheme {
        TotalBalanceSection(balance = 1250.75)
    }
}

@Preview(showBackground = true)
@Composable
fun PeriodChartPreview() {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val sampleTransactions = listOf(
        Transaction(1, TransactionType.Ingreso, 500.0, 1, now, "Income"),
        Transaction(2, TransactionType.Gasto, 200.0, 2, now, "Expense"),
        Transaction(3, TransactionType.Ingreso, 300.0, 1, now - 30 * dayMs, "Past Income"),
        Transaction(4, TransactionType.Gasto, 150.0, 2, now - 30 * dayMs, "Past Expense")
    )
    EasyExpenseControlTheme {
        PeriodChart(
            transactions = sampleTransactions,
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LatestTransactionsListPreview() {
    val sampleCategories = listOf(
        Category(1, "Alimentos", TransactionType.Gasto, "restaurant"),
        Category(2, "Sueldo", TransactionType.Ingreso, "payments")
    )
    val now = System.currentTimeMillis()
    val sampleTransactions = listOf(
        Transaction(1, TransactionType.Ingreso, 2500.0, 2, now, "Sueldo Mensual"),
        Transaction(2, TransactionType.Gasto, 45.50, 1, now, "Cena fuera")
    )
    EasyExpenseControlTheme {
        LatestTransactionsList(
            transactions = sampleTransactions,
            categories = sampleCategories,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LatestTransactionRowPreview() {
    val sampleCategory = Category(1, "Alimentos", TransactionType.Gasto, "restaurant")
    val sampleTransaction = Transaction(2, TransactionType.Gasto, 45.50, 1, System.currentTimeMillis(), "Cena fuera")
    EasyExpenseControlTheme {
        LatestTransactionRow(
            transaction = sampleTransaction,
            category = sampleCategory
        )
    }
}

