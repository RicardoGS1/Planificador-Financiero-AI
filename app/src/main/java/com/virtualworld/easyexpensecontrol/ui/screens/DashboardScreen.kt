package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.core.util.getLastThreeDays
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppBarView
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import java.util.Locale

private const val MAX_ULTIMAS_ENTRADAS = 15

@Composable
fun DashboardScreen(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel
) {
    Scaffold(
        topBar = {
            AppBarView(title = "Planificador Financiero", showBackArrow = false)
        },
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
            TotalBalanceSection(balance = balance)
            Spacer(modifier = Modifier.height(12.dp))
            LastThreeDaysChart(
                transactions = listaTransacciones,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Últimas entradas",
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
            text = "Saldo total",
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
fun LastThreeDaysChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val days = getLastThreeDays()
    val dayData = days.map { (label, dayStartMs) ->
        val dayEndMs = dayStartMs + 86400000L
        val ingresos = transactions
            .filter { it.type == TransactionType.Ingreso && it.date >= dayStartMs && it.date < dayEndMs }
            .sumOf { it.amount }
        val gastos = transactions
            .filter { it.type == TransactionType.Gasto && it.date >= dayStartMs && it.date < dayEndMs }
            .sumOf { it.amount }
        Triple(label, ingresos, gastos)
    }
    val maxVal = dayData.flatMap { listOf(it.second, it.third) }.maxOrNull() ?: 1.0
    val maxHeight = maxVal.coerceAtLeast(1.0)

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
            Text(
                text = "Últimos 3 días",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
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

                dayData.forEachIndexed { index, (_, ing, gas) ->
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
                days.forEach { (label, _) ->
                    Text(
                        text = label,
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
                    Text("Ingresos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.size(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFE33936), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Gastos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
                    text = "No hay transacciones recientes",
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
                imageVector = if (isIngreso) Icons.Default.ArrowDownward else Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifEmpty { "Sin descripción" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = category.name.ifEmpty { "Sin categoría" },
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
