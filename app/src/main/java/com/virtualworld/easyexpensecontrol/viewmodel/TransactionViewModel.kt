package com.virtualworld.easyexpensecontrol.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getTransactionsByCategoryAndDateUseCase: GetTransactionsByCategoryAndDateUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {
    var transactionTypeState by mutableStateOf(TransactionType.Ingreso)
    var transactionAmountState by mutableDoubleStateOf(0.0)
    var transactionCategoryState by mutableLongStateOf(0L)
    var transactionDateState by mutableLongStateOf(0L)
    var transactionDescriptionState by mutableStateOf("")

    fun onTransactionTypeChanged(newType: TransactionType) {
        transactionTypeState = newType
    }

    fun onTransactionAmountChanged(newAmount: Double) {
        transactionAmountState = newAmount
    }

    fun onTransactionDateChanged(newDate: Long) {
        transactionDateState = newDate
    }

    fun onTransactionDescriptionChanged(newString: String) {
        transactionDescriptionState = newString
    }

    val getAllTransactions: Flow<List<Transaction>> = getTransactionsUseCase()

    fun getTransactionById(id: Long): Flow<Transaction> = getTransactionByIdUseCase(id)

    fun deleteTransactionAndCheckCategory(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTransactionUseCase(transaction)
        }
    }

    fun getTransactionsByCategoryAndDate(categoryId: Long, year: Int, month: String): Flow<List<Transaction>> =
        getTransactionsByCategoryAndDateUseCase(categoryId, year, month)

    fun saveTransaction(
        id: Long,
        categoryName: String,
        category: Category?,
        onError: suspend (String) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            saveTransactionUseCase(
                id = id,
                type = transactionTypeState,
                amount = transactionAmountState,
                description = transactionDescriptionState.trim(),
                categoryName = categoryName,
                category = category,
                date = transactionDateState,
                onError = { msg -> viewModelScope.launch(Dispatchers.Main) { onError(msg) } },
                onSuccess = { viewModelScope.launch(Dispatchers.Main) { onSuccess() } }
            )
        }
    }
}
