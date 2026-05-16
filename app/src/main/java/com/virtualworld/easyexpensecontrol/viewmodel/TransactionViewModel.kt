package com.virtualworld.easyexpensecontrol.viewmodel

import android.content.Context
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
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessAudioUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessReceiptUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import com.virtualworld.easyexpensecontrol.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getTransactionsByCategoryAndDateUseCase: GetTransactionsByCategoryAndDateUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val processAudioUseCase: ProcessAudioUseCase,
    private val getCategoryByNameUseCase: GetCategoryByNameUseCase,
    private val appContext: Context
) : ViewModel() {
    var transactionTypeState by mutableStateOf<TransactionType?>(null)
    var transactionAmountState by mutableDoubleStateOf(0.0)
    var transactionCategoryState by mutableLongStateOf(0L)
    var transactionDateState by mutableLongStateOf(0L)
    var transactionDescriptionState by mutableStateOf("")

    private val _receiptProcessingState = MutableStateFlow<ReceiptProcessingState>(ReceiptProcessingState.Idle)
    val receiptProcessingState: StateFlow<ReceiptProcessingState> = _receiptProcessingState.asStateFlow()

    fun onTransactionTypeChanged(newType: TransactionType?) {
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

    fun processReceiptImage(imageBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            handleAnalysisResult(processReceiptUseCase(imageBytes))
        }
    }

    fun processAudio(audioBytes: ByteArray, type: TransactionType, mimeType: String = "audio/aac") {
        viewModelScope.launch(Dispatchers.IO) {
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            handleAnalysisResult(processAudioUseCase(audioBytes, type, mimeType))
        }
    }

    private suspend fun handleAnalysisResult(result: Result<com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult>) {
        result
            .onSuccess { data ->
                val category = getCategoryByNameUseCase(data.suggestedCategoryName).firstOrNull()
                val categoryNameForUi = if (category != null) {
                    withContext(Dispatchers.Main) {
                        transactionAmountState = data.amount
                        transactionDescriptionState = data.description
                        transactionCategoryState = category.id
                    }
                    category.name
                } else {
                    withContext(Dispatchers.Main) {
                        transactionAmountState = data.amount
                        transactionDescriptionState = data.description
                        transactionCategoryState = 0L
                    }
                    data.suggestedCategoryName
                }
                _receiptProcessingState.value = ReceiptProcessingState.Success(categoryNameForUi)
            }
            .onFailure { e ->
                _receiptProcessingState.value = ReceiptProcessingState.Error(
                    e.message ?: appContext.getString(R.string.error_receipt_analysis)
                )
            }
    }

    fun clearReceiptProcessingState() {
        _receiptProcessingState.value = ReceiptProcessingState.Idle
    }

    fun saveTransaction(
        id: Long,
        categoryName: String,
        category: Category?,
        iconName: String?,
        onError: suspend (String) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            saveTransactionUseCase(
                id = id,
                type = transactionTypeState ?: return@launch,
                amount = transactionAmountState,
                description = transactionDescriptionState.trim(),
                categoryName = categoryName,
                category = category,
                date = transactionDateState,
                iconName = iconName,
                onError = { msg -> viewModelScope.launch(Dispatchers.Main) { onError(msg) } },
                onSuccess = { viewModelScope.launch(Dispatchers.Main) { onSuccess() } }
            )
        }
    }
}
