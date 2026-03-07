package com.virtualworld.easyexpensecontrol.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Budget
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.components.MonthPicker
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddEditDetailBudgetView(
    id: Long,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    transactionViewModel: TransactionViewModel,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    if (id != 0L) {
        val transaction = budgetViewModel.getBudgetById(id).collectAsState(initial = Budget(0L, 0L, 0.0, 0.0, "0", 0))
        transaction.value.let {
            budgetViewModel.budgetCategoryState = it.category
            budgetViewModel.budgetCurrentExpenditureState = it.currentExpenditure
            budgetViewModel.budgetMonthlyLimitState = it.monthlyLimit
            budgetViewModel.budgetMonthState = it.month
            budgetViewModel.budgetYearState = it.year
        }

        val category = categoryViewModel.getCategoryById(budgetViewModel.budgetCategoryState).collectAsState(initial = null)
        LaunchedEffect(category.value) {
            category.value?.let {
                categoryViewModel.categoryNameState = it.name
                categoryViewModel.categoryTypeState = it.type
            }
        }
    } else {
        budgetViewModel.budgetCategoryState = 0L
        budgetViewModel.budgetCurrentExpenditureState = 0.0
        budgetViewModel.budgetMonthlyLimitState = 0.0
    }

    fun handleSaveBudget() {
        scope.launch {
            try {
                val totalExpenditure = withContext(Dispatchers.IO) {
                    transactionViewModel
                        .getTransactionsByCategoryAndDate(
                            budgetViewModel.budgetCategoryState,
                            budgetViewModel.budgetYearState,
                            budgetViewModel.budgetMonthState.padStart(2, '0')
                        )
                        .first()
                        .sumOf { it.amount }
                }

                val budget = Budget(
                    id = if (id != 0L) id else 0L,
                    category = budgetViewModel.budgetCategoryState,
                    monthlyLimit = budgetViewModel.budgetMonthlyLimitState,
                    currentExpenditure = totalExpenditure,
                    month = budgetViewModel.budgetMonthState.padStart(2, '0'),
                    year = budgetViewModel.budgetYearState
                )

                withContext(Dispatchers.IO) {
                    if (id != 0L) {
                        budgetViewModel.updateBudget(budget)
                    } else {
                        budgetViewModel.addBudget(budget)
                    }
                }

                snackbarHostState.showSnackbar("Operación completada con éxito.")
                navController.navigateUp()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                ScreenHeader(
                    title = if (id != 0L) {
                        stringResource(id = R.string.update_budget)
                    } else {
                        stringResource(id = R.string.add_budget)
                    },
                    showBackArrow = true,
                    onBackClick = { navController.navigateUp() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selecciona la categoria ")
                    BudgetCategoryDropdown(
                        selectedCategory = budgetViewModel.budgetCategoryState,
                        categoryViewModel = categoryViewModel,
                        onCategoryChanged = budgetViewModel::onBudgetCategoryChanged
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                var isPickerVisible by remember { mutableStateOf(false) }
                var date by remember { mutableStateOf("") }

                FilledTonalButton(onClick = { isPickerVisible = true }) {
                    Text("Seleccionar Mes y Año")
                }

                if (id == 0L) {
                    if (isPickerVisible) {
                        MonthPicker(
                            visible = isPickerVisible,
                            currentMonth = budgetViewModel.budgetMonthState.toInt(),
                            currentYear = budgetViewModel.budgetYearState,
                            confirmButtonCLicked = { month, year ->
                                budgetViewModel.onBudgetMonthChanged(month.toString())
                                budgetViewModel.onBudgetYearChanged(year)
                                val monthFormatted = month.toString().padStart(2, '0')
                                date = "$monthFormatted/$year"
                                isPickerVisible = false
                            },
                            cancelClicked = {
                                isPickerVisible = false
                            }
                        )
                    }
                    if (date.isBlank()) {
                        Text(text = "Mes y Año no seleccionado.")
                    } else {
                        Text(text = "Mes y Año selecionado: $date")
                    }
                } else {
                    if (isPickerVisible) {
                        MonthPicker(
                            visible = isPickerVisible,
                            currentMonth = budgetViewModel.budgetMonthState.toInt(),
                            currentYear = budgetViewModel.budgetYearState,
                            confirmButtonCLicked = { month, year ->
                                budgetViewModel.onBudgetMonthChanged(month.toString())
                                budgetViewModel.onBudgetYearChanged(year)
                                val monthFormatted = month.toString().padStart(2, '0')
                                date = "$monthFormatted/$year"
                                isPickerVisible = false
                            },
                            cancelClicked = {
                                isPickerVisible = false
                            }
                        )
                    }
                    Text(text = "Mes y Año selecionado: ${budgetViewModel.budgetMonthState}/${budgetViewModel.budgetYearState}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    label = "Limite mensual",
                    value = budgetViewModel.budgetMonthlyLimitState.toString(),
                    onValueChange = { value ->
                        budgetViewModel.onBudgetMonthlyLimitChanged(value.toDoubleOrNull() ?: 0.0)
                    },
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        when {
                            budgetViewModel.budgetCategoryState == 0L -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Por favor, selecciona una categoría para continuar.")
                                }
                            }

                            id == 0L && date.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Por favor, selecciona un mes y año para continuar.")
                                }
                            }

                            budgetViewModel.budgetMonthlyLimitState == 0.0 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("El campo de límite mensual no puede estar vacío.")
                                }
                            }

                            id == 0L -> {
                                isLoading = true
                                scope.launch {
                                    val budgetAlreadyExists = budgetViewModel
                                        .getBudgetForCategoryMonthAndYear(
                                            budgetViewModel.budgetCategoryState,
                                            budgetViewModel.budgetMonthState,
                                            budgetViewModel.budgetYearState
                                        ).first()
                                    if (budgetAlreadyExists != null) {
                                        snackbarHostState.showSnackbar("No se pueden agregar dos presupuestos idénticos.")
                                    } else {
                                        handleSaveBudget()
                                    }
                                }
                            }

                            else -> {
                                isLoading = true
                                handleSaveBudget()
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (id != 0L) stringResource(id = R.string.update_budget)
                            else stringResource(id = R.string.add_budget),
                            style = TextStyle(fontSize = 18.sp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BudgetCategoryDropdown(
    selectedCategory: Long,
    categoryViewModel: CategoryViewModel,
    onCategoryChanged: (Long) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val categories = categoryViewModel.getCategoriesByType(TransactionType.Gasto).collectAsState(initial = emptyList()).value
    val selectedCategoryName = categories.find { it.id == selectedCategory }?.name ?: "Seleccionalo aquí"

    Card {
        OutlinedButton(onClick = { expanded.value = !expanded.value }) {
            Text(text = selectedCategoryName)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            categories.forEach { category ->
                DropdownMenuItem(onClick = {
                    onCategoryChanged(category.id)
                    expanded.value = false
                }) {
                    Text(text = category.name)
                }
            }
        }
    }
}
