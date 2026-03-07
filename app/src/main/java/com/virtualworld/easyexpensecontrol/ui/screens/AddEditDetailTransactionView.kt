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
import androidx.compose.material3.ElevatedButton
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
import com.virtualworld.easyexpensecontrol.core.util.convertTimestampToString
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.components.DatePickerModal
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

@Composable
fun AddEditDetailTransactionView(
    id: Long,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }

    if (id != 0L) {
        val transaction = transactionViewModel.getTransactionById(id).collectAsState(initial = Transaction(0L, TransactionType.Ingreso, 0.0, 0L, 0L, ""))
        transaction.value.let {
            transactionViewModel.transactionTypeState = it.type
            transactionViewModel.transactionAmountState = it.amount
            transactionViewModel.transactionDescriptionState = it.description
            transactionViewModel.transactionCategoryState = it.category
            transactionViewModel.transactionDateState = it.date
        }

        val category = categoryViewModel.getCategoryById(transactionViewModel.transactionCategoryState).collectAsState(initial = null)
        LaunchedEffect(category.value) {
            category.value?.let {
                categoryName = it.name
                categoryViewModel.categoryNameState = it.name
                categoryViewModel.categoryTypeState = it.type
            }
        }
    } else {
        transactionViewModel.transactionTypeState = TransactionType.Ingreso
        transactionViewModel.transactionAmountState = 0.0
        transactionViewModel.transactionDescriptionState = ""
        transactionViewModel.transactionCategoryState = 0L
        transactionViewModel.transactionDateState = 0L
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        content = { it ->
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                ScreenHeader(
                    title = if (id != 0L) {
                        stringResource(id = R.string.update_transaction)
                    } else {
                        stringResource(id = R.string.add_transaction)
                    },
                    showBackArrow = true,
                    onBackClick = { navController.navigateUp() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selecciona el tipo de transacción ")
                    TransactionTypeDropdown(
                        selectedType = transactionViewModel.transactionTypeState,
                        onTypeChanged = transactionViewModel::onTransactionTypeChanged
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    label = "Importe",
                    value = transactionViewModel.transactionAmountState.toString(),
                    onValueChange = { value ->
                        transactionViewModel.onTransactionAmountChanged(value.toDoubleOrNull() ?: 0.0)
                    },
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    label = "Descripción",
                    value = transactionViewModel.transactionDescriptionState,
                    onValueChange = transactionViewModel::onTransactionDescriptionChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    label = "Categoría",
                    value = categoryName,
                    onValueChange = { value ->
                        categoryName = value
                    }
                )

                val category by categoryViewModel.getCategoryByName(categoryName).collectAsState(initial = null)

                Spacer(modifier = Modifier.height(16.dp))

                var showDatePicker by remember { mutableStateOf(false) }

                if (showDatePicker) {
                    DatePickerModal(
                        initialDate = transactionViewModel.transactionDateState.takeIf { it != 0L },
                        onDateSelected = { selectedDate ->
                            transactionViewModel.onTransactionDateChanged(selectedDate ?: 0L)
                            showDatePicker = false
                        },
                        onDismiss = {
                            showDatePicker = false
                        }
                    )
                }

                ElevatedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        text = "Abrir Selector de Fecha",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                Text(
                    text = "Fecha: ${transactionViewModel.transactionDateState.takeIf { it != 0L }
                        ?.let { convertTimestampToString(it) } ?: "No seleccionada"}"
                )

                Spacer(modifier = Modifier.height(16.dp))

                var isLoading by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        when {
                            transactionViewModel.transactionAmountState <= 0 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("El importe debe ser mayor a 0.")
                                }
                            }
                            transactionViewModel.transactionDescriptionState.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("La descripción no puede estar vacía.")
                                }
                            }
                            categoryName.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("El campo de categoría no puede estar vacío.")
                                }
                            }
                            transactionViewModel.transactionDateState == 0L -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Debe seleccionar una fecha.")
                                }
                            }
                            else -> {
                                isLoading = true
                                transactionViewModel.saveTransaction(
                                    id = id,
                                    categoryName = categoryName,
                                    category = category,
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error: $error")
                                            isLoading = false
                                        }
                                    },
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Operación completada con éxito")
                                            navController.navigateUp()
                                        }
                                    }
                                )
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
                            text = if (id != 0L) stringResource(id = R.string.update_transaction)
                            else stringResource(id = R.string.add_transaction),
                            style = TextStyle(fontSize = 18.sp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TransactionTypeDropdown(
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val types = TransactionType.entries.toTypedArray()

    Card {
        OutlinedButton(onClick = { expanded.value = !expanded.value }) {
            Text(text = selectedType.name)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            types.forEach { type ->
                DropdownMenuItem(onClick = {
                    onTypeChanged(type)
                    expanded.value = false
                }) {
                    Text(text = type.name)
                }
            }
        }
    }
}
