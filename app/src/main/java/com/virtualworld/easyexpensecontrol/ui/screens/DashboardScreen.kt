package com.virtualworld.easyexpensecontrol.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeDayPeriods
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeMonthPeriods
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeWeekPeriods
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Locale

private enum class ChartPeriod { DAY, WEEK, MONTH }

private const val MAX_ULTIMAS_ENTRADAS = 15

@Composable
fun DashboardScreen(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val listaTransacciones = transactionViewModel.getAllTransactions
            .collectAsState(initial = emptyList()).value

        var income = 0.0
        var expenses = 0.0
        listaTransacciones.forEach { t ->
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
                        onClick = { navController.navigate(Screen.SettingsScreen.route) },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
            TotalBalanceSection(balance = balance)
            Spacer(modifier = Modifier.height(12.dp))
            PeriodChart(
                transactions = listaTransacciones,
                todayLabel = stringResource(R.string.day_today),
                yesterdayLabel = stringResource(R.string.day_yesterday),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.last_entries),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LatestTransactionsList(
                transactions = listaTransacciones,
                categoryViewModel = categoryViewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun TotalBalanceSection(balance: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.total_balance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "%.2f €".format(Locale.getDefault(), balance),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
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

    val dayLabel = stringResource(R.string.period_day)
    val weekLabel = stringResource(R.string.period_week)
    val monthLabel = stringResource(R.string.period_month)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_income) + " / " + stringResource(R.string.label_expense),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.size(8.dp))
                PeriodSelector(
                    selected = selectedPeriod,
                    onSelect = { selectedPeriod = it },
                    dayLabel = dayLabel,
                    weekLabel = weekLabel,
                    monthLabel = monthLabel
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val barGroupWidth = size.width / 3f
                val barWidth = barGroupWidth / 3f
                val bottomPadding = 28f
                val chartHeight = size.height - bottomPadding
                val gap = barWidth * 0.2f

                periodData.forEachIndexed { index, (_, ing, gas) ->
                    val groupLeft = index * barGroupWidth
                    val ingHeight = (ing / maxHeight).toFloat().coerceIn(0f, 1f) * chartHeight
                    val gasHeight = (gas / maxHeight).toFloat().coerceIn(0f, 1f) * chartHeight

                    val bar1Left = groupLeft + gap
                    val bar2Left = groupLeft + barWidth + gap * 2

                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(bar1Left, chartHeight - ingHeight),
                        size = Size(barWidth - gap, ingHeight.coerceAtLeast(4f))
                    )
                    drawRect(
                        color = Color(0xFFE33936),
                        topLeft = Offset(bar2Left, chartHeight - gasHeight),
                        size = Size(barWidth - gap, gasHeight.coerceAtLeast(4f))
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEach { period ->
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.label_income), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.size(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFE33936), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.label_expense), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(2.dp)
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(period) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    val sorted = transactions.sortedByDescending { it.date }.take(MAX_ULTIMAS_ENTRADAS)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(sorted, key = { _, t -> t.id }) { index, transaction ->
                    LatestTransactionRow(
                        transaction = transaction,
                        categoryViewModel = categoryViewModel
                    )
                    if (index < sorted.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
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
    val categoryFlow = categoryViewModel.getCategoryById(transaction.category)
    val category = categoryFlow.collectAsState(initial = Category(0L, "", TransactionType.Ingreso)).value
    val isIngreso = transaction.type == TransactionType.Ingreso
    val displayIcon = if (isIngreso) Icons.Default.ArrowDownward else CategoryIcons.getIcon(category.iconName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AccentBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifEmpty { stringResource(R.string.no_description) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = category.name.ifEmpty { stringResource(R.string.no_category) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = if (isIngreso) "+%.2f €".format(Locale.getDefault(), transaction.amount)
            else "-%.2f €".format(Locale.getDefault(), transaction.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isIngreso) Color(0xFF4CAF50) else Color(0xFFE33936)
        )
    }
}
