package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.convertTimestampToString
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppBarView
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.components.NavigationBar
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryScreen(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    budgetViewModel: BudgetViewModel,
    onPlaySound: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppBarView(
                title = "Transacciones",
                showBackArrow = false
            ) { navController.navigateUp() }
        },
        bottomBar = { NavigationBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        content = { paddingValues ->
            val transactionList = transactionViewModel.getAllTransactions
                .collectAsState(initial = emptyList())

            if (transactionList.value.isEmpty()) {
                Text(
                    text = "No hay transacciones disponibles.",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(transactionList.value, key = { transaction -> transaction.id }) { transaction ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
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
                                        contentDescription = "Delete Icon",
                                        tint = colorResource(R.color.blue_white)
                                    )
                                }
                            } else if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Icon",
                                        tint = colorResource(R.color.blue_white)
                                    )
                                }
                            }
                        },
                        enableDismissFromEndToStart = true,
                        enableDismissFromStartToEnd = true,
                        content = {
                            TransactionItem(transaction = transaction, categoryViewModel = categoryViewModel) {
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
            title = { Text("¿Desea eliminar esta transacción?") },
            text = {
                Text(
                    "Esta acción conlleva que esta transacción y toda la información relacionada con ella se elimine permanentemente de la aplicación."
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
                    Text("Aceptar", color = colorResource(R.color.red_transaction))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancelar")
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

@Composable
fun TransactionItem(
    transaction: Transaction,
    categoryViewModel: CategoryViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.description,
                        color = colorResource(id = R.color.bold_from_palette),
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (transaction.type.name == "Ingreso") "${transaction.amount}€" else "-${transaction.amount}€",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type.name == "Ingreso") {
                        colorResource(id = R.color.green_transaction)
                    } else {
                        colorResource(id = R.color.red_transaction)
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            val categoryFlow = categoryViewModel.getCategoryById(transaction.category)
            val categoryState = categoryFlow.collectAsState(initial = Category(0L, "", TransactionType.Ingreso))
            val currentCategory = categoryState.value

            Text(
                text = "Categoría: " + currentCategory.name,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(id = R.color.blue_green_light)
            )
            Text(
                text = convertTimestampToString(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(id = R.color.blue_ultra_light)
            )
        }
    }
}
