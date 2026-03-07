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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppBarView
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import com.virtualworld.easyexpensecontrol.ui.components.CurvedBottomBar
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel

@Composable
fun BudgetScreen(
    navController: NavHostController,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    onPlaySound: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        topBar = {
            AppBarView(title = "Presupuestos", showBackArrow = false) {
                navController.navigateUp()
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        bottomBar = { CurvedBottomBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val budgetList = budgetViewModel.getAllBudgets.collectAsState(initial = emptyList())

        if (budgetList.value.isEmpty()) {
            Text(
                text = "No hay presupuestos disponibles. Por favor antes de añadir presupuestos, añada transacciones.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
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
            item {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalButton(onClick = {
                            navController.navigate(Screen.AddEditBudgetScreen.route + "/0L")
                        }) {
                            Text("Añadir presupuesto")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            items(budgetList.value, key = { budget -> budget.id }) { budget ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            budgetToDelete = budget
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
                        BudgetItem(
                            budget = budget,
                            categoryViewModel = categoryViewModel
                        ) {
                            val id = budget.id
                            navController.navigate(Screen.AddEditBudgetScreen.route + "/$id")
                        }
                    }
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Desea eliminar este presupuesto?") },
            text = {
                Text(
                    "Esta acción conlleva que este presupuesto y toda la información relacionada con él se elimine permanentemente de la aplicación."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        budgetToDelete?.let { budgetViewModel.deleteBudget(it) }
                        onPlaySound(R.raw.delete_sound)
                        showDialog = false
                    }
                ) {
                    Text("Aceptar", color = colorResource(R.color.red_transaction))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BudgetItem(
    budget: Budget,
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
            val categoryFlow = categoryViewModel.getCategoryById(budget.category)
            val categoryState = categoryFlow.collectAsState(initial = Category(0L, "", TransactionType.Ingreso))
            val currentCategory = categoryState.value

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentCategory.name,
                    color = colorResource(id = R.color.bold_from_palette),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${budget.currentExpenditure}€ / ${budget.monthlyLimit}€",
                    fontWeight = FontWeight.Bold,
                    color = if (budget.currentExpenditure > budget.monthlyLimit) {
                        colorResource(id = R.color.red_transaction)
                    } else {
                        colorResource(id = R.color.bold_from_palette)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = (budget.currentExpenditure.toFloat() / budget.monthlyLimit.toFloat())
            LinearProgressIndicator(
                progress = minOf(progress, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (progress > 1f) {
                    colorResource(id = R.color.red_transaction)
                } else {
                    colorResource(id = R.color.green_transaction)
                }
            )

            Text(
                text = "${budget.month}/${budget.year}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(id = R.color.blue_ultra_light)
            )
        }
    }
}
