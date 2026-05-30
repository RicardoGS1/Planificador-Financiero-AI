package com.virtualworld.easyexpensecontrol.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme
import com.virtualworld.easyexpensecontrol.ui.theme.EmbeddedControlActive
import com.virtualworld.easyexpensecontrol.ui.theme.EmbeddedControlBackground
import com.virtualworld.easyexpensecontrol.ui.theme.EmbeddedControlBorder
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import com.virtualworld.easyexpensecontrol.core.util.getEndOfDay
import com.virtualworld.easyexpensecontrol.core.util.getEndOfMonth
import com.virtualworld.easyexpensecontrol.core.util.getEndOfYear
import com.virtualworld.easyexpensecontrol.core.util.getLastNDays
import com.virtualworld.easyexpensecontrol.core.util.getStartOfMonth
import com.virtualworld.easyexpensecontrol.core.util.getStartOfYear
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.components.AccountFilterDropdown
import com.virtualworld.easyexpensecontrol.ui.components.ALL_ACCOUNTS_FILTER_ID
import com.virtualworld.easyexpensecontrol.ui.components.filterTransactionsByAccount
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class PeriodType { Day, Month, Year }
private val StatisticsFiltersCardShape = RoundedCornerShape(20.dp)
private const val STATICS_PREFS_NAME = "statistics_screen_preferences"
private const val KEY_PERIOD_TYPE = "period_type"
private const val KEY_SELECTED_DAY_INDEX = "selected_day_index"
private const val KEY_SELECTED_MONTH = "selected_month"
private const val KEY_SELECTED_YEAR = "selected_year"

private data class ChartBarGroup(val label: String, val income: Double, val expense: Double)

private data class CategoryExpenseSlice(
    val categoryId: Long,
    val name: String,
    val amount: Double
)

private var hasPlayedDashboardBarDropAnimation = false

private val ExpenseCategoryPalette: List<Color> = listOf(
    Color(0xFFEF4444),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF8B5CF6),
    Color(0xFFF97316),
    Color(0xFF6366F1),
    Color(0xFFE33974),
    Color(0xFFA855F7),
    Color(0xFF11A5BF),
    Color(0xFF3B82F6),
    Color(0xFF14B8A6),
    Color(0xFF22C55E),
    Color(0xFF0EA5E9),
    Color(0xFF84CC16),
    Color(0xFF10B981)
)

private val IncomeCategoryPalette: List<Color> = listOf(
    Color(0xFF22C55E),
    Color(0xFF10B981),
    Color(0xFF14B8A6),
    Color(0xFF11A5BF),
    Color(0xFF0EA5E9),
    Color(0xFF3B82F6),
    Color(0xFF6366F1),
    Color(0xFF84CC16),
    Color(0xFF06B6D4),
    Color(0xFF8B5CF6),
    Color(0xFFA855F7),
    Color(0xFF4ADE80),
    Color(0xFF2DD4BF),
    Color(0xFF38BDF8),
    Color(0xFF818CF8)
)

@Composable
fun StaticsScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    accountViewModel: AccountViewModel
) {
    val transactions by transactionViewModel.getAllTransactions.collectAsState(initial = emptyList())
    val categories by categoryViewModel.getAllCategories.collectAsState(initial = emptyList())
    val accounts by accountViewModel.visibleAccounts.collectAsState(initial = emptyList())

    StaticsScreen(
        navController = navController,
        transactions = transactions,
        categories = categories,
        accounts = accounts
    )
}

@Composable
fun StaticsScreen(
    navController: NavController,
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<com.virtualworld.easyexpensecontrol.data.model.Account>
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.applicationContext.getSharedPreferences(STATICS_PREFS_NAME, Context.MODE_PRIVATE)
    }
    val currentCalendar = remember { Calendar.getInstance() }
    var selectedAccountFilter by remember { mutableStateOf(ALL_ACCOUNTS_FILTER_ID) }

    LaunchedEffect(accounts, selectedAccountFilter) {
        if (selectedAccountFilter != ALL_ACCOUNTS_FILTER_ID &&
            accounts.none { it.id == selectedAccountFilter }
        ) {
            selectedAccountFilter = ALL_ACCOUNTS_FILTER_ID
        }
    }

    val transactionsList = remember(transactions, selectedAccountFilter) {
        filterTransactionsByAccount(transactions, selectedAccountFilter)
    }
    val categoriesList = categories

    var periodType by remember {
        mutableStateOf(
            preferences.getString(KEY_PERIOD_TYPE, PeriodType.Month.name)
                ?.let { stored -> PeriodType.entries.firstOrNull { it.name == stored } }
                ?: PeriodType.Month
        )
    }
    var selectedDayIndex by remember {
        mutableIntStateOf(preferences.getInt(KEY_SELECTED_DAY_INDEX, 0).coerceAtLeast(0))
    }
    var selectedMonth by remember {
        mutableIntStateOf(
            preferences.getInt(
                KEY_SELECTED_MONTH,
                currentCalendar.get(Calendar.MONTH) + 1
            ).coerceIn(1, 12)
        )
    }
    var selectedYear by remember {
        mutableIntStateOf(
            preferences.getInt(
                KEY_SELECTED_YEAR,
                currentCalendar.get(Calendar.YEAR)
            )
        )
    }

    val todayLabel = stringResource(R.string.day_today)
    val yesterdayLabel = stringResource(R.string.day_yesterday)
    val monthNamesShort = stringArrayResource(R.array.month_names_short).toList()
    val days = remember(todayLabel, yesterdayLabel) { getLastNDays(30, todayLabel, yesterdayLabel) }

    LaunchedEffect(days.size) {
        if (days.isNotEmpty()) {
            selectedDayIndex = selectedDayIndex.coerceIn(0, days.lastIndex)
        } else {
            selectedDayIndex = 0
        }
    }

    LaunchedEffect(periodType, selectedDayIndex, selectedMonth, selectedYear) {
        preferences.edit()
            .putString(KEY_PERIOD_TYPE, periodType.name)
            .putInt(KEY_SELECTED_DAY_INDEX, selectedDayIndex)
            .putInt(KEY_SELECTED_MONTH, selectedMonth)
            .putInt(KEY_SELECTED_YEAR, selectedYear)
            .apply()
    }

    val selectedDayStart = days.getOrNull(selectedDayIndex)?.second ?: 0L
    val selectedDayEnd = if (selectedDayStart > 0) getEndOfDay(selectedDayStart) else 0L

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = stringResource(R.string.screen_statistics), showBackArrow = false)

            StatisticsFiltersCard(
                accounts = accounts,
                selectedAccountFilter = selectedAccountFilter,
                onAccountSelected = { selectedAccountFilter = it },
                periodType = periodType,
                onPeriodTypeSelect = { periodType = it },
                days = days,
                selectedDayIndex = selectedDayIndex,
                onDaySelect = { selectedDayIndex = it },
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onMonthChange = { selectedMonth = it },
                onYearChange = { selectedYear = it }
            )

            if (transactionsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.statistics_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val chartData = when (periodType) {
                    PeriodType.Day -> {
                        if (selectedDayStart == 0L) emptyList()
                        else {
                            val ing = transactionsList
                                .filter { it.type == TransactionType.Ingreso && it.date in selectedDayStart..selectedDayEnd }
                                .sumOf { it.amount }
                            val gas = transactionsList
                                .filter { it.type == TransactionType.Gasto && it.date in selectedDayStart..selectedDayEnd }
                                .sumOf { it.amount }
                            val label = days.getOrNull(selectedDayIndex)?.first ?: ""
                            listOf(ChartBarGroup(label, ing, gas))
                        }
                    }
                    PeriodType.Month -> {
                        val startMs = getStartOfMonth(selectedYear, selectedMonth)
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startMs
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        (1..daysInMonth).map { day ->
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val dStart = cal.timeInMillis
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.set(Calendar.MILLISECOND, 999)
                            val dEnd = cal.timeInMillis
                            val ing = transactionsList
                                .filter { it.type == TransactionType.Ingreso && it.date in dStart..dEnd }
                                .sumOf { it.amount }
                            val gas = transactionsList
                                .filter { it.type == TransactionType.Gasto && it.date in dStart..dEnd }
                                .sumOf { it.amount }
                            ChartBarGroup(day.toString(), ing, gas)
                        }
                    }
                    PeriodType.Year -> {
                        (1..12).map { month ->
                            val mStart = getStartOfMonth(selectedYear, month)
                            val mEnd = getEndOfMonth(selectedYear, month)
                            val ing = transactionsList
                                .filter { it.type == TransactionType.Ingreso && it.date in mStart..mEnd }
                                .sumOf { it.amount }
                            val gas = transactionsList
                                .filter { it.type == TransactionType.Gasto && it.date in mStart..mEnd }
                                .sumOf { it.amount }
                            ChartBarGroup(monthNamesShort[month - 1], ing, gas)
                        }
                    }
                }

                StatisticsChart(
                    data = chartData,
                    periodType = periodType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                val (rangeStart, rangeEnd) = when (periodType) {
                    PeriodType.Day -> selectedDayStart to selectedDayEnd
                    PeriodType.Month -> getStartOfMonth(selectedYear, selectedMonth) to
                        getEndOfMonth(selectedYear, selectedMonth)
                    PeriodType.Year -> getStartOfYear(selectedYear) to getEndOfYear(selectedYear)
                }

                val noCategoryLabel = stringResource(R.string.no_category)
                val categoryById = remember(categoriesList) { categoriesList.associateBy { it.id } }

                val categoryExpenses: List<CategoryExpenseSlice> = if (rangeStart == 0L) {
                    emptyList()
                } else {
                    transactionsList
                        .asSequence()
                        .filter { it.type == TransactionType.Gasto && it.date in rangeStart..rangeEnd }
                        .groupBy { it.category }
                        .map { (categoryId, txList) ->
                            val name = categoryById[categoryId]?.name ?: noCategoryLabel
                            CategoryExpenseSlice(
                                categoryId = categoryId,
                                name = name,
                                amount = txList.sumOf { it.amount }
                            )
                        }
                        .filter { it.amount > 0.0 }
                        .sortedByDescending { it.amount }
                }

                val categoryIncomes: List<CategoryExpenseSlice> = if (rangeStart == 0L) {
                    emptyList()
                } else {
                    transactionsList
                        .asSequence()
                        .filter { it.type == TransactionType.Ingreso && it.date in rangeStart..rangeEnd }
                        .groupBy { it.category }
                        .map { (categoryId, txList) ->
                            val name = categoryById[categoryId]?.name ?: noCategoryLabel
                            CategoryExpenseSlice(
                                categoryId = categoryId,
                                name = name,
                                amount = txList.sumOf { it.amount }
                            )
                        }
                        .filter { it.amount > 0.0 }
                        .sortedByDescending { it.amount }
                }

                CategoryPieChartCard(
                    title = stringResource(R.string.chart_subtitle_categories),
                    subtitle = stringResource(R.string.chart_categories_breakdown),
                    centerLabel = stringResource(R.string.label_expense),
                    emptyMessage = stringResource(R.string.chart_no_expenses_period),
                    slices = categoryExpenses,
                    palette = ExpenseCategoryPalette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )

                CategoryPieChartCard(
                    title = stringResource(R.string.chart_subtitle_income_categories),
                    subtitle = stringResource(R.string.chart_categories_breakdown),
                    centerLabel = stringResource(R.string.label_income),
                    emptyMessage = stringResource(R.string.chart_no_income_period),
                    slices = categoryIncomes,
                    palette = IncomeCategoryPalette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatisticsFiltersCard(
    accounts: List<com.virtualworld.easyexpensecontrol.data.model.Account>,
    selectedAccountFilter: Long,
    onAccountSelected: (Long) -> Unit,
    periodType: PeriodType,
    onPeriodTypeSelect: (PeriodType) -> Unit,
    days: List<Pair<String, Long>>,
    selectedDayIndex: Int,
    onDaySelect: (Int) -> Unit,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            AccentBlue.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = StatisticsFiltersCardShape,
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
            PeriodTypeSelector(
                selected = periodType,
                onSelect = onPeriodTypeSelect,
                embedded = true,
                modifier = Modifier.fillMaxWidth()
            )
            when (periodType) {
                PeriodType.Day -> DaySelector(
                    days = days,
                    selectedIndex = selectedDayIndex,
                    onSelect = onDaySelect,
                    embedded = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PeriodType.Month -> MonthYearSelector(
                    month = selectedMonth,
                    year = selectedYear,
                    onMonthChange = onMonthChange,
                    onYearChange = onYearChange,
                    embedded = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PeriodType.Year -> YearSelector(
                    year = selectedYear,
                    onYearChange = onYearChange,
                    embedded = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PeriodTypeSelector(
    selected: PeriodType,
    onSelect: (PeriodType) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = if (embedded) EmbeddedControlBackground else MaterialTheme.colorScheme.surface
    val onSurface = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface
    val onPrimary = if (embedded) primary else Color.White
    val outline = if (embedded) {
        EmbeddedControlBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val activeContainer = if (embedded) EmbeddedControlActive else primary
    val cornerRadius = 6.dp

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        SegmentedButton(
            selected = selected == PeriodType.Day,
            onClick = { onSelect(PeriodType.Day) },
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                bottomStart = cornerRadius,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            ),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = activeContainer,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface,
                activeBorderColor = if (embedded) Color.White else primary,
                inactiveBorderColor = outline
            )
        ) {
            Text(stringResource(R.string.period_day))
        }
        SegmentedButton(
            selected = selected == PeriodType.Month,
            onClick = { onSelect(PeriodType.Month) },
            shape = RoundedCornerShape(0.dp),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = activeContainer,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface,
                activeBorderColor = if (embedded) Color.White else primary,
                inactiveBorderColor = outline
            )
        ) {
            Text(stringResource(R.string.period_month))
        }
        SegmentedButton(
            selected = selected == PeriodType.Year,
            onClick = { onSelect(PeriodType.Year) },
            shape = RoundedCornerShape(
                topStart = 0.dp,
                bottomStart = 0.dp,
                topEnd = cornerRadius,
                bottomEnd = cornerRadius
            ),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = activeContainer,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface,
                activeBorderColor = if (embedded) Color.White else primary,
                inactiveBorderColor = outline
            )
        ) {
            Text(stringResource(R.string.period_year))
        }
    }
}

private val DateSelectorPanelShape = RoundedCornerShape(10.dp)

@Composable
private fun DateSelectorLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun CompactDateNavButton(
    onClick: () -> Unit,
    contentDescription: String,
    tint: Color,
    forward: Boolean
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (forward) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DaySelector(
    days: List<Pair<String, Long>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val labelColor = if (embedded) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentColor = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface
    val panelBackground = if (embedded) {
        EmbeddedControlBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val panelBorder = if (embedded) {
        EmbeddedControlBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val contentPadding = if (embedded) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

    Column(modifier = modifier.then(contentPadding)) {
        DateSelectorLabel(
            text = stringResource(R.string.select_day),
            color = labelColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DateSelectorPanelShape)
                .background(panelBackground)
                .border(1.dp, panelBorder, DateSelectorPanelShape)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactDateNavButton(
                onClick = { if (selectedIndex < days.size - 1) onSelect(selectedIndex + 1) },
                contentDescription = stringResource(R.string.cd_day_previous),
                tint = contentColor,
                forward = false
            )
            Text(
                text = days.getOrNull(selectedIndex)?.first ?: "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            CompactDateNavButton(
                onClick = { if (selectedIndex > 0) onSelect(selectedIndex - 1) },
                contentDescription = stringResource(R.string.cd_day_next),
                tint = contentColor,
                forward = true
            )
        }
    }
}

@Composable
private fun MonthYearSelector(
    month: Int,
    year: Int,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val monthNames = stringArrayResource(R.array.month_names_short).toList()
    val labelColor = if (embedded) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentColor = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface
    val panelBackground = if (embedded) {
        EmbeddedControlBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val panelBorder = if (embedded) EmbeddedControlBorder else null
    val contentPadding = if (embedded) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

    Column(modifier = modifier.then(contentPadding)) {
        DateSelectorLabel(
            text = stringResource(R.string.month_year),
            color = labelColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DateSelectorPanelShape)
                .background(panelBackground)
                .then(
                    if (panelBorder != null) {
                        Modifier.border(1.dp, panelBorder, DateSelectorPanelShape)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactDateNavButton(
                    onClick = {
                        if (month <= 1) {
                            onYearChange(year - 1)
                            onMonthChange(12)
                        } else {
                            onMonthChange(month - 1)
                        }
                    },
                    contentDescription = stringResource(R.string.cd_month_previous),
                    tint = contentColor,
                    forward = false
                )
                Text(
                    text = monthNames.getOrNull(month - 1) ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )
                CompactDateNavButton(
                    onClick = {
                        if (month >= 12) {
                            onYearChange(year + 1)
                            onMonthChange(1)
                        } else {
                            onMonthChange(month + 1)
                        }
                    },
                    contentDescription = stringResource(R.string.cd_month_next),
                    tint = contentColor,
                    forward = true
                )

            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactDateNavButton(
                    onClick = { onYearChange(year - 1) },
                    contentDescription = stringResource(R.string.cd_year_previous),
                    tint = contentColor,
                    forward = false
                )
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.Center
                )
                CompactDateNavButton(
                    onClick = { onYearChange(year + 1) },
                    contentDescription = stringResource(R.string.cd_year_next),
                    tint = contentColor,
                    forward = true
                )

            }
        }
    }
}

@Composable
private fun YearSelector(
    year: Int,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val labelColor = if (embedded) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val contentColor = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface
    val panelBackground = if (embedded) {
        EmbeddedControlBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val panelBorder = if (embedded) EmbeddedControlBorder else null
    val contentPadding = if (embedded) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

    Column(modifier = modifier.then(contentPadding)) {
        DateSelectorLabel(
            text = stringResource(R.string.year_label),
            color = labelColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DateSelectorPanelShape)
                .background(panelBackground)
                .then(
                    if (panelBorder != null) {
                        Modifier.border(1.dp, panelBorder, DateSelectorPanelShape)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactDateNavButton(
                onClick = { onYearChange(year - 1) },
                contentDescription = stringResource(R.string.cd_year_previous),
                tint = contentColor,
                forward = false
            )
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            CompactDateNavButton(
                onClick = { onYearChange(year + 1) },
                contentDescription = stringResource(R.string.cd_year_next),
                tint = contentColor,
                forward = true
            )
        }
    }
}

@Composable
private fun StatisticsChart(
    data: List<ChartBarGroup>,
    periodType: PeriodType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencySymbol = CurrencyFormatter.symbol(context)
    val colorGreen = colorResource(R.color.green_transaction)
    val colorRed = colorResource(R.color.red_transaction)
    val labelColor = colorResource(R.color.bold_from_palette)

    val subtitle = when (periodType) {
        PeriodType.Day -> stringResource(R.string.chart_subtitle_day)
        PeriodType.Month -> stringResource(R.string.chart_subtitle_month)
        PeriodType.Year -> stringResource(R.string.chart_subtitle_year)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(R.string.comparison_currency, currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.chart_no_data_period),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AttractiveBarChart(
                    data = data,
                    colorIncome = colorGreen,
                    colorExpense = colorRed,
                    currencySymbol = currencySymbol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (data.size <= 2) 180.dp else 200.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = colorGreen, label = stringResource(R.string.label_income))
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendItem(color = colorRed, label = stringResource(R.string.label_expense))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AttractiveBarChart(
    data: List<ChartBarGroup>,
    colorIncome: Color,
    colorExpense: Color,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val maxVal = data.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0
    val maxHeight = maxVal.coerceAtLeast(1.0).toFloat()
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 28f
            color = android.graphics.Color.DKGRAY
        }
    }
    val shouldPlayIntroAnimation = remember(data) { !hasPlayedDashboardBarDropAnimation && data.isNotEmpty() }
    var barProgressTarget by remember(data) { mutableStateOf(if (shouldPlayIntroAnimation) 0f else 1f) }
    val barAnimationDurations = remember(data) {
        val seed = data.fold(17L) { acc, group ->
            val incomePart = (group.income * 100).roundToInt().toLong()
            val expensePart = (group.expense * 100).roundToInt().toLong()
            acc * 31 + group.label.hashCode() + incomePart + expensePart
        }
        val random = Random(seed)
        List(data.size * 2) { random.nextInt(from = 650, until = 1550) }
    }
    val barProgressValues = barAnimationDurations.mapIndexed { index, durationMillis ->
        animateFloatAsState(
            targetValue = barProgressTarget,
            animationSpec = tween(durationMillis = durationMillis),
            label = "dashboard_bar_drop_progress_$index"
        ).value
    }

    LaunchedEffect(data) {
        if (shouldPlayIntroAnimation) {
            hasPlayedDashboardBarDropAnimation = true
            barProgressTarget = 1f
        }
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val leftPadding = 48f
            val bottomPadding = 36f
            val rightPadding = 16f
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - bottomPadding
            val steps = 4
            val cornerRadius = 8f

            // Grid y ejes
            for (i in 0..steps) {
                val y = chartHeight * (1 - i.toFloat() / steps)
                val value = (maxHeight * i / steps).roundToInt()
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.6f),
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawIntoCanvas { canvas ->
                    paint.color = android.graphics.Color.GRAY
                    canvas.nativeCanvas.drawText(
                        "$value$currencySymbol",
                        4f,
                        y + 10f,
                        paint
                    )
                }
            }
            drawLine(
                color = Color.Gray.copy(alpha = 0.8f),
                start = Offset(leftPadding, 0f),
                end = Offset(leftPadding, chartHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.8f),
                start = Offset(leftPadding, chartHeight),
                end = Offset(size.width - rightPadding, chartHeight),
                strokeWidth = 2f
            )

            val groupCount = data.size.coerceAtLeast(1)
            val groupWidth = chartWidth / groupCount
            val barGap = (groupWidth * 0.15f).coerceAtLeast(2f)
            val barWidth = (groupWidth - barGap * 2) / 2f

            data.forEachIndexed { index, group ->
                val groupLeft = leftPadding + index * groupWidth
                val targetIncomeHeight =
                    ((group.income.toFloat() / maxHeight).coerceIn(0f, 1f) * chartHeight).coerceAtLeast(4f)
                val targetExpenseHeight =
                    ((group.expense.toFloat() / maxHeight).coerceIn(0f, 1f) * chartHeight).coerceAtLeast(4f)
                val incomeProgress = barProgressValues.getOrElse(index * 2) { 1f }
                val expenseProgress = barProgressValues.getOrElse(index * 2 + 1) { 1f }
                val ingHeight = if (shouldPlayIntroAnimation) {
                    chartHeight + (targetIncomeHeight - chartHeight) * incomeProgress
                } else {
                    targetIncomeHeight
                }
                val gasHeight = if (shouldPlayIntroAnimation) {
                    chartHeight + (targetExpenseHeight - chartHeight) * expenseProgress
                } else {
                    targetExpenseHeight
                }

                val bar1Left = groupLeft + barGap
                val bar2Left = groupLeft + barGap + barWidth + barGap

                drawRoundRect(
                    color = colorIncome,
                    topLeft = Offset(bar1Left, chartHeight - ingHeight),
                    size = Size(barWidth, ingHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
                drawRoundRect(
                    color = colorExpense,
                    topLeft = Offset(bar2Left, chartHeight - gasHeight),
                    size = Size(barWidth, gasHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )

                // Etiqueta bajo el grupo
                val label = group.label
                if (label.isNotEmpty()) {
                    drawIntoCanvas { canvas ->
                        paint.color = android.graphics.Color.DKGRAY
                        paint.textSize = 22f
                        val textWidth = paint.measureText(label)
                        canvas.nativeCanvas.drawText(
                            label,
                            groupLeft + (groupWidth - textWidth) / 2f,
                            chartHeight + 26f,
                            paint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    StatisticsChart(
        data = listOf(ChartBarGroup("", income, expenses)),
        periodType = PeriodType.Day,
        modifier = modifier
    )
}

@Composable
fun PieChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    var startAngle = 0f

    Canvas(modifier = modifier.fillMaxWidth()) {
        data.forEach { (category, value) ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[data.keys.indexOf(category) % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp
) {
    if (dataPoints.isEmpty()) return

    val max = dataPoints.maxOrNull() ?: 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val stepX = size.width / (dataPoints.size - 1).coerceAtLeast(1)

        for (i in 0 until dataPoints.size - 1) {
            val startX = i * stepX
            val startY = size.height - (dataPoints[i] / max) * size.height
            val endX = (i + 1) * stepX
            val endY = size.height - (dataPoints[i + 1] / max) * size.height

            drawLine(
                color = lineColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth.toPx()
            )
        }
    }
}

@Composable
private fun CategoryPieChartCard(
    title: String,
    subtitle: String,
    centerLabel: String,
    emptyMessage: String,
    slices: List<CategoryExpenseSlice>,
    palette: List<Color>,
    modifier: Modifier = Modifier
) {
    val labelColor = colorResource(R.color.bold_from_palette)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val total = slices.sumOf { it.amount }
    val sliceColors = remember(slices, palette) {
        slices.mapIndexed { index, _ -> palette[index % palette.size] }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (slices.isEmpty() || total <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChartCanvas(
                        slices = slices,
                        colors = sliceColors,
                        ringColor = surfaceColor,
                        modifier = Modifier
                            .size(220.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = centerLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(LocalContext.current, total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    slices.forEachIndexed { index, slice ->
                        val percent = if (total > 0.0) (slice.amount / total * 100.0).toFloat() else 0f
                        CategoryLegendRow(
                            color = sliceColors[index],
                            name = slice.name,
                            amount = slice.amount,
                            percent = percent
                        )
                        if (index < slices.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<CategoryExpenseSlice>,
    colors: List<Color>,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.amount }.coerceAtLeast(0.0001)

    Canvas(modifier = modifier) {
        val diameter = min(size.width, size.height)
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)
        val gapDeg = if (slices.size > 1) 1.5f else 0f
        var startAngle = -90f

        slices.forEachIndexed { index, slice ->
            val rawSweep = ((slice.amount / total) * 360.0).toFloat()
            val sweep = (rawSweep - gapDeg).coerceAtLeast(0.5f)
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = diameter * 0.18f)
            )
            startAngle += rawSweep
        }

        drawCircle(
            color = ringColor,
            radius = diameter * 0.34f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

@Composable
private fun CategoryLegendRow(
    color: Color,
    name: String,
    amount: Double,
    percent: Float
) {
    val currencySymbol = CurrencyFormatter.symbol(LocalContext.current)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = stringResource(
                R.string.chart_category_amount_percent,
                amount,
                currencySymbol,
                percent
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StaticsScreenPreview() {
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
        StaticsScreen(
            navController = rememberNavController(),
            transactions = sampleTransactions,
            categories = sampleCategories,
            accounts = listOf(com.virtualworld.easyexpensecontrol.data.model.Account(1, "General"))
        )
    }
}
