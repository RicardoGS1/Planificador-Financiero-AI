package com.virtualworld.easyexpensecontrol.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.getEndOfDay
import com.virtualworld.easyexpensecontrol.core.util.getEndOfMonth
import com.virtualworld.easyexpensecontrol.core.util.getEndOfYear
import com.virtualworld.easyexpensecontrol.core.util.getLastNDays
import com.virtualworld.easyexpensecontrol.core.util.getMonthNamesShort
import com.virtualworld.easyexpensecontrol.core.util.getStartOfMonth
import com.virtualworld.easyexpensecontrol.core.util.getStartOfYear
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private enum class PeriodType { Day, Month, Year }

private data class ChartBarGroup(val label: String, val income: Double, val expense: Double)

@Composable
fun StaticsScreen(navController: NavController, transactionViewModel: TransactionViewModel) {
    val transactions = transactionViewModel.getAllTransactions
        .collectAsState(initial = emptyList()).value

    var periodType by remember { mutableStateOf(PeriodType.Day) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val days = remember { getLastNDays(30) }
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
            ScreenHeader(title = "Estadísticas", showBackArrow = false)

            // Selector de período: Día | Mes | Año
            PeriodTypeSelector(
                selected = periodType,
                onSelect = { periodType = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            when (periodType) {
                PeriodType.Day -> DaySelector(
                    days = days,
                    selectedIndex = selectedDayIndex,
                    onSelect = { selectedDayIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )
                PeriodType.Month -> MonthYearSelector(
                    month = selectedMonth,
                    year = selectedYear,
                    onMonthChange = { selectedMonth = it },
                    onYearChange = { selectedYear = it },
                    modifier = Modifier.fillMaxWidth()
                )
                PeriodType.Year -> YearSelector(
                    year = selectedYear,
                    onYearChange = { selectedYear = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos. Añade transacciones para ver estadísticas.",
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
                            val ing = transactions
                                .filter { it.type == TransactionType.Ingreso && it.date in selectedDayStart..selectedDayEnd }
                                .sumOf { it.amount }
                            val gas = transactions
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
                            val ing = transactions
                                .filter { it.type == TransactionType.Ingreso && it.date in dStart..dEnd }
                                .sumOf { it.amount }
                            val gas = transactions
                                .filter { it.type == TransactionType.Gasto && it.date in dStart..dEnd }
                                .sumOf { it.amount }
                            ChartBarGroup(day.toString(), ing, gas)
                        }
                    }
                    PeriodType.Year -> {
                        val monthNames = getMonthNamesShort()
                        (1..12).map { month ->
                            val mStart = getStartOfMonth(selectedYear, month)
                            val mEnd = getEndOfMonth(selectedYear, month)
                            val ing = transactions
                                .filter { it.type == TransactionType.Ingreso && it.date in mStart..mEnd }
                                .sumOf { it.amount }
                            val gas = transactions
                                .filter { it.type == TransactionType.Gasto && it.date in mStart..mEnd }
                                .sumOf { it.amount }
                            ChartBarGroup(monthNames[month - 1], ing, gas)
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
            }
        }
    }
}

@Composable
private fun PeriodTypeSelector(
    selected: PeriodType,
    onSelect: (PeriodType) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = colorResource(R.color.blue_dark)
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = Color.White

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        SegmentedButton(
            selected = selected == PeriodType.Day,
            onClick = { onSelect(PeriodType.Day) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = primary,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface
            )
        ) {
            Text("Día")
        }
        SegmentedButton(
            selected = selected == PeriodType.Month,
            onClick = { onSelect(PeriodType.Month) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = primary,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface
            )
        ) {
            Text("Mes")
        }
        SegmentedButton(
            selected = selected == PeriodType.Year,
            onClick = { onSelect(PeriodType.Year) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = primary,
                activeContentColor = onPrimary,
                inactiveContainerColor = surface,
                inactiveContentColor = onSurface
            )
        ) {
            Text("Año")
        }
    }
}

@Composable
private fun DaySelector(
    days: List<Pair<String, Long>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Selecciona el día",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (selectedIndex < days.size - 1) onSelect(selectedIndex + 1) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Día anterior")
            }
            Text(
                text = days.getOrNull(selectedIndex)?.first ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { if (selectedIndex > 0) onSelect(selectedIndex - 1) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Día siguiente")
            }
        }
    }
}

@Composable
private fun MonthYearSelector(
    month: Int,
    year: Int,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = getMonthNamesShort()
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Mes y año",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (month >= 12) {
                        onYearChange(year + 1)
                        onMonthChange(1)
                    } else {
                        onMonthChange(month + 1)
                    }
                }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Mes siguiente", modifier = Modifier.rotate(-90f))
                }
                Text(
                    text = monthNames.getOrNull(month - 1) ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = {
                    if (month <= 1) {
                        onYearChange(year - 1)
                        onMonthChange(12)
                    } else {
                        onMonthChange(month - 1)
                    }
                }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Mes anterior", modifier = Modifier.rotate(90f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onYearChange(year + 1) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Año siguiente")
                }
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onYearChange(year - 1) }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Año anterior")
                }
            }
        }
    }
}

@Composable
private fun YearSelector(
    year: Int,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Año",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(year - 1) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Año anterior")
            }
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            IconButton(onClick = { onYearChange(year + 1) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Año siguiente")
            }
        }
    }
}

@Composable
private fun StatisticsChart(
    data: List<ChartBarGroup>,
    periodType: PeriodType,
    modifier: Modifier = Modifier
) {
    val colorGreen = colorResource(R.color.green_transaction)
    val colorRed = colorResource(R.color.red_transaction)
    val labelColor = colorResource(R.color.bold_from_palette)

    val subtitle = when (periodType) {
        PeriodType.Day -> "Ingresos vs Gastos del día"
        PeriodType.Month -> "Ingresos y gastos por día"
        PeriodType.Year -> "Ingresos y gastos por mes"
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
                text = "Comparación en €",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos en este período",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AttractiveBarChart(
                    data = data,
                    colorIncome = colorGreen,
                    colorExpense = colorRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (data.size <= 2) 260.dp else 280.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = colorGreen, label = "Ingresos")
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendItem(color = colorRed, label = "Gastos")
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
                        "${value}€",
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
                val ingHeight = (group.income.toFloat() / maxHeight).coerceIn(0f, 1f) * chartHeight
                val gasHeight = (group.expense.toFloat() / maxHeight).coerceIn(0f, 1f) * chartHeight

                val bar1Left = groupLeft + barGap
                val bar2Left = groupLeft + barGap + barWidth + barGap

                drawRoundRect(
                    color = colorIncome,
                    topLeft = Offset(bar1Left, chartHeight - ingHeight),
                    size = Size(barWidth, ingHeight.coerceAtLeast(4f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
                drawRoundRect(
                    color = colorExpense,
                    topLeft = Offset(bar2Left, chartHeight - gasHeight),
                    size = Size(barWidth, gasHeight.coerceAtLeast(4f)),
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
