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
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessAudioUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessReceiptUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.SensitiveDataSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetectedTransactionItem(
    val amount: Double,
    val description: String,
    val categoryName: String,
    val categoryId: Long = 0L
)

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

    private val _detectedTransactions = MutableStateFlow<List<DetectedTransactionItem>>(emptyList())
    val detectedTransactions: StateFlow<List<DetectedTransactionItem>> = _detectedTransactions.asStateFlow()

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
            handleAnalysisResult(
                result = processReceiptUseCase(imageBytes),
                defaultType = TransactionType.Gasto
            )
        }
    }

    fun processAudio(audioBytes: ByteArray, type: TransactionType, mimeType: String = "audio/aac") {
        viewModelScope.launch(Dispatchers.IO) {
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            handleAnalysisResult(
                result = processAudioUseCase(audioBytes, type, mimeType),
                defaultType = type
            )
        }
    }

    private suspend fun handleAnalysisResult(
        result: Result<ReceiptResult>,
        defaultType: TransactionType
    ) {
        result
            .onSuccess { data ->
                if (data.items.isEmpty()) {
                    _receiptProcessingState.value = ReceiptProcessingState.Error(
                        appContext.getString(R.string.gemini_empty_response)
                    )
                    return
                }

                val detected = data.items.map { item ->
                    val category = getCategoryByNameUseCase(item.suggestedCategoryName).firstOrNull()
                    DetectedTransactionItem(
                        amount = item.amount,
                        description = item.description,
                        categoryName = category?.name ?: item.suggestedCategoryName,
                        categoryId = category?.id ?: 0L
                    )
                }
                val first = detected.first()
                withContext(Dispatchers.Main) {
                    if (transactionTypeState == null) {
                        transactionTypeState = defaultType
                    }
                    _detectedTransactions.value = _detectedTransactions.value + detected
                    loadDetectedTransaction(first)
                }
                _receiptProcessingState.value = ReceiptProcessingState.Success(detected.size)
            }
            .onFailure { e ->
                val rawMessage = e.message ?: appContext.getString(R.string.error_receipt_analysis)
                _receiptProcessingState.value = ReceiptProcessingState.Error(
                    SensitiveDataSanitizer.sanitize(rawMessage)
                )
            }
    }

    fun loadDetectedTransaction(item: DetectedTransactionItem) {
        transactionAmountState = item.amount
        transactionDescriptionState = item.description
        transactionCategoryState = item.categoryId
    }

    fun clearReceiptProcessingState() {
        _receiptProcessingState.value = ReceiptProcessingState.Idle
    }

    fun clearDetectedTransactions() {
        _detectedTransactions.value = emptyList()
    }

    fun resetFormForNewTransaction() {
        transactionAmountState = 0.0
        transactionDescriptionState = ""
        transactionCategoryState = 0L
    }

    fun removeDetectedTransactionAt(index: Int): Boolean {
        val current = _detectedTransactions.value.toMutableList()
        if (index !in current.indices) return false
        current.removeAt(index)
        _detectedTransactions.value = current
        return true
    }

    fun addOrUpdateDetectedTransaction(
        categoryName: String,
        categoryId: Long,
        updateIndex: Int?,
        onSuccess: () -> Unit
    ) {
        val item = DetectedTransactionItem(
            amount = transactionAmountState,
            description = transactionDescriptionState.trim(),
            categoryName = categoryName.trim(),
            categoryId = categoryId
        )
        val current = _detectedTransactions.value.toMutableList()
        if (updateIndex != null && updateIndex in current.indices) {
            current[updateIndex] = item
        } else {
            current.add(item)
        }
        _detectedTransactions.value = current
        resetFormForNewTransaction()
        onSuccess()
    }

    fun commitDetectedTransactions(
        onError: suspend (String) -> Unit,
        onSuccess: suspend (Int) -> Unit
    ) {
        val type = transactionTypeState ?: run {
            viewModelScope.launch { onSuccess(0) }
            return
        }
        val date = transactionDateState
        val items = _detectedTransactions.value.toList()
        if (items.isEmpty()) {
            viewModelScope.launch { onSuccess(0) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            for ((index, item) in items.withIndex()) {
                val category = getCategoryByNameUseCase(item.categoryName).firstOrNull()
                val saveResult = saveTransactionInternal(
                    type = type,
                    amount = item.amount,
                    description = item.description,
                    categoryName = item.categoryName,
                    category = category,
                    date = date,
                    iconName = null
                )
                if (saveResult.isFailure) {
                    val message = saveResult.exceptionOrNull()?.message.orEmpty()
                    _detectedTransactions.value = items.drop(index)
                    withContext(Dispatchers.Main) {
                        onError(if (message.isBlank()) appContext.getString(R.string.error_unknown) else message)
                    }
                    return@launch
                }
            }
            _detectedTransactions.value = emptyList()
            withContext(Dispatchers.Main) {
                onSuccess(items.size)
            }
        }
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
            val type = transactionTypeState ?: return@launch
            val amount = transactionAmountState
            val description = transactionDescriptionState.trim()
            val date = transactionDateState
            val trimmedCategoryName = categoryName.trim()

            val saveResult = saveTransactionInternal(
                id = id,
                type = type,
                amount = amount,
                description = description,
                categoryName = trimmedCategoryName,
                category = category,
                date = date,
                iconName = iconName
            )
            if (saveResult.isFailure) {
                val message = saveResult.exceptionOrNull()?.message.orEmpty()
                withContext(Dispatchers.Main) {
                    onError(if (message.isBlank()) appContext.getString(R.string.error_unknown) else message)
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    private suspend fun saveTransactionInternal(
        id: Long = 0L,
        type: TransactionType,
        amount: Double,
        description: String,
        categoryName: String,
        category: Category?,
        date: Long,
        iconName: String?
    ): Result<Unit> {
        var errorMessage: String? = null
        saveTransactionUseCase(
            id = id,
            type = type,
            amount = amount,
            description = description,
            categoryName = categoryName,
            category = category,
            date = date,
            iconName = iconName,
            onError = { message -> errorMessage = message },
            onSuccess = { }
        )
        return errorMessage?.let { Result.failure(IllegalStateException(it)) } ?: Result.success(Unit)
    }
}
