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
import com.virtualworld.easyexpensecontrol.data.model.RecurringTransaction
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.domain.model.ReceiptResult
import com.virtualworld.easyexpensecontrol.core.util.CategoryNameMatcher
import com.virtualworld.easyexpensecontrol.core.util.AccountNameMatcher
import com.virtualworld.easyexpensecontrol.core.util.AiDateParser
import com.virtualworld.easyexpensecontrol.core.util.AiPromptBuilder
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetVisibleAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessAudioUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessReceiptUseCase
import com.virtualworld.easyexpensecontrol.core.util.ImportedFileType
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessSpreadsheetUseCase
import java.time.Instant
import java.time.ZoneId
import com.virtualworld.easyexpensecontrol.core.util.RecurringDateHelper
import com.virtualworld.easyexpensecontrol.core.util.getTodayStartOfDay
import com.virtualworld.easyexpensecontrol.domain.usecase.recurring.GetRecurringTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.recurring.ProcessRecurringTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsEvents
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsManager
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
    val categoryId: Long = 0L,
    val date: Long? = null,
    val transactionType: TransactionType? = null,
    val accountId: Long? = null,
    val isRecurring: Boolean = false
)

class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getTransactionsByCategoryAndDateUseCase: GetTransactionsByCategoryAndDateUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val processRecurringTransactionsUseCase: ProcessRecurringTransactionsUseCase,
    private val getRecurringTransactionByIdUseCase: GetRecurringTransactionByIdUseCase,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val processAudioUseCase: ProcessAudioUseCase,
    private val processSpreadsheetUseCase: ProcessSpreadsheetUseCase,
    private val getCategoryByNameUseCase: GetCategoryByNameUseCase,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
    private val appContext: Context,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {
    var transactionTypeState by mutableStateOf<TransactionType?>(null)
    var transactionAmountState by mutableDoubleStateOf(0.0)
    var transactionCategoryState by mutableLongStateOf(0L)
    var transactionDateState by mutableLongStateOf(0L)
    var transactionDescriptionState by mutableStateOf("")
    var transactionAccountState by mutableLongStateOf(1L)
    var isRecurringEnabledState by mutableStateOf(false)
    var editingRecurringTransactionId by mutableStateOf<Long?>(null)

    private val _receiptProcessingState = MutableStateFlow<ReceiptProcessingState>(ReceiptProcessingState.Idle)
    val receiptProcessingState: StateFlow<ReceiptProcessingState> = _receiptProcessingState.asStateFlow()

    private val _detectedTransactions = MutableStateFlow<List<DetectedTransactionItem>>(emptyList())
    val detectedTransactions: StateFlow<List<DetectedTransactionItem>> = _detectedTransactions.asStateFlow()

    private val _pendingAutoGeneratedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val pendingAutoGeneratedTransactions: StateFlow<List<Transaction>> = _pendingAutoGeneratedTransactions.asStateFlow()

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

    fun onTransactionAccountChanged(accountId: Long) {
        transactionAccountState = accountId
    }

    fun onRecurringEnabledChanged(enabled: Boolean) {
        isRecurringEnabledState = enabled
    }

    fun clearPendingAutoGenerated() {
        _pendingAutoGeneratedTransactions.value = emptyList()
    }

    suspend fun processRecurringOnStartup() {
        val generated = withContext(Dispatchers.IO) {
            processRecurringTransactionsUseCase()
        }
        if (generated.isNotEmpty()) {
            _pendingAutoGeneratedTransactions.value = generated
        }
    }

    suspend fun loadRecurringStateForEdit(transaction: Transaction) {
        editingRecurringTransactionId = transaction.recurringTransactionId
        if (transaction.recurringTransactionId != null) {
            val template = getRecurringTransactionByIdUseCase(transaction.recurringTransactionId)
            isRecurringEnabledState = template?.isActive ?: false
        } else {
            isRecurringEnabledState = false
        }
    }

    fun observeRecurringTransaction(id: Long): Flow<RecurringTransaction?> =
        getRecurringTransactionByIdUseCase.observe(id)

    suspend fun hasTemplateChanges(
        recurringId: Long,
        type: TransactionType,
        amount: Double,
        description: String,
        categoryId: Long,
        accountId: Long,
        date: Long
    ): Boolean {
        val template = getRecurringTransactionByIdUseCase(recurringId) ?: return false
        return template.type != type ||
            template.amount != amount ||
            template.description != description ||
            template.categoryId != categoryId ||
            template.accountId != accountId ||
            template.dayOfMonth != RecurringDateHelper.dayOfMonthFromMillis(date)
    }

    val getAllTransactions: Flow<List<Transaction>> = getTransactionsUseCase()

    fun getTransactionById(id: Long): Flow<Transaction> = getTransactionByIdUseCase(id)

    fun deleteTransactionAndCheckCategory(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTransactionUseCase(transaction)
            analyticsManager.logTransactionDeleted(transaction.type)
        }
    }

    fun getTransactionsByCategoryAndDate(categoryId: Long, year: Int, month: String): Flow<List<Transaction>> =
        getTransactionsByCategoryAndDateUseCase(categoryId, year, month)

    fun processReceiptImage(imageBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            analyticsManager.logAiAnalysisStarted(AiAnalysisSource.RECEIPT)
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            handleAnalysisResult(
                result = processReceiptUseCase(imageBytes),
                defaultType = TransactionType.Gasto,
                source = AiAnalysisSource.RECEIPT
            )
        }
    }

    fun processAudio(audioBytes: ByteArray, type: TransactionType?, mimeType: String = "audio/aac") {
        viewModelScope.launch(Dispatchers.IO) {
            analyticsManager.logAiAnalysisStarted(AiAnalysisSource.AUDIO)
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            handleAnalysisResult(
                result = processAudioUseCase(audioBytes, type, mimeType),
                defaultType = type ?: TransactionType.Gasto,
                source = AiAnalysisSource.AUDIO
            )
        }
    }

    fun processImportedFile(
        fileBytes: ByteArray,
        fileType: ImportedFileType,
        startDateMillis: Long,
        endDateMillis: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            analyticsManager.logAiAnalysisStarted(AiAnalysisSource.SPREADSHEET)
            _receiptProcessingState.value = ReceiptProcessingState.Loading
            val zone = ZoneId.systemDefault()
            val startIso = Instant.ofEpochMilli(startDateMillis).atZone(zone).toLocalDate().toString()
            val endIso = Instant.ofEpochMilli(endDateMillis).atZone(zone).toLocalDate().toString()
            handleAnalysisResult(
                result = processSpreadsheetUseCase(fileBytes, fileType, startIso, endIso),
                defaultType = TransactionType.Gasto,
                source = AiAnalysisSource.SPREADSHEET
            )
        }
    }

    private suspend fun handleAnalysisResult(
        result: Result<ReceiptResult>,
        defaultType: TransactionType,
        source: AiAnalysisSource
    ) {
        result
            .onSuccess { data ->
                if (data.items.isEmpty()) {
                    analyticsManager.logAiAnalysisResult(
                        source = source,
                        success = false,
                        errorCategory = AnalyticsEvents.ERROR_EMPTY
                    )
                    _receiptProcessingState.value = ReceiptProcessingState.Error(
                        appContext.getString(R.string.gemini_empty_response)
                    )
                    return
                }

                val categories = getCategoriesByTypeUseCase(defaultType).firstOrNull().orEmpty()
                val spreadsheetCategories = if (source == AiAnalysisSource.SPREADSHEET) {
                    val expense = getCategoriesByTypeUseCase(TransactionType.Gasto).firstOrNull().orEmpty()
                    val income = getCategoriesByTypeUseCase(TransactionType.Ingreso).firstOrNull().orEmpty()
                    expense + income
                } else {
                    emptyList()
                }
                val accounts = getVisibleAccountsUseCase().firstOrNull().orEmpty()
                val defaultOtherLabel = appContext.getString(R.string.category_default_other)
                val detected = data.items.map { item ->
                    val itemTypeFromAi = AiPromptBuilder.parseTransactionType(
                        raw = item.suggestedTransactionType,
                        fallback = defaultType
                    )
                    val itemCategories = when {
                        source == AiAnalysisSource.SPREADSHEET -> spreadsheetCategories
                        item.suggestedTransactionType.isNotBlank() ->
                            getCategoriesByTypeUseCase(itemTypeFromAi).firstOrNull().orEmpty()
                        else -> categories
                    }
                    val matched = CategoryNameMatcher.resolve(
                        suggestedName = item.suggestedCategoryName,
                        categories = itemCategories,
                        defaultOtherLabel = defaultOtherLabel
                    ) ?: getCategoryByNameUseCase(item.suggestedCategoryName).firstOrNull()
                    val itemType = when {
                        item.suggestedTransactionType.isNotBlank() -> itemTypeFromAi
                        matched != null -> matched.type
                        else -> itemTypeFromAi
                    }
                    val matchedAccount = AccountNameMatcher.resolve(item.suggestedAccountName, accounts)
                    DetectedTransactionItem(
                        amount = item.amount,
                        description = item.description,
                        categoryName = matched?.name ?: item.suggestedCategoryName,
                        categoryId = matched?.id ?: 0L,
                        date = AiDateParser.parseIsoDateOrNull(item.suggestedDateIso),
                        transactionType = itemType,
                        accountId = matchedAccount?.id
                    )
                }
                val first = detected.first()
                withContext(Dispatchers.Main) {
                    first.transactionType?.let { transactionTypeState = it }
                    if (transactionTypeState == null) {
                        transactionTypeState = defaultType
                    }
                    first.date?.let { transactionDateState = it }
                    first.accountId?.let { transactionAccountState = it }
                    _detectedTransactions.value = _detectedTransactions.value + detected
                    loadDetectedTransaction(first)
                }
                _receiptProcessingState.value = ReceiptProcessingState.Success(
                    transactionCount = detected.size,
                    source = source
                )
                analyticsManager.logAiAnalysisResult(
                    source = source,
                    success = true,
                    transactionCount = detected.size
                )
            }
            .onFailure { e ->
                analyticsManager.logAiAnalysisResult(
                    source = source,
                    success = false,
                    errorCategory = analyticsManager.categorizeAiError(e)
                )
                val fallback = when (source) {
                    AiAnalysisSource.SPREADSHEET -> when (e.message) {
                        "empty_file", "empty_spreadsheet" ->
                            appContext.getString(R.string.error_spreadsheet_empty)
                        else -> appContext.getString(R.string.error_spreadsheet_analysis)
                    }
                    else -> appContext.getString(R.string.error_receipt_analysis)
                }
                val rawMessage = e.message?.takeIf { it.isNotBlank() } ?: fallback
                _receiptProcessingState.value = ReceiptProcessingState.Error(
                    SensitiveDataSanitizer.sanitize(rawMessage)
                )
            }
    }

    fun loadDetectedTransaction(item: DetectedTransactionItem) {
        transactionAmountState = item.amount
        transactionDescriptionState = item.description
        transactionCategoryState = item.categoryId
        item.transactionType?.let { transactionTypeState = it }
        item.date?.let { transactionDateState = it }
        item.accountId?.let { transactionAccountState = it }
        isRecurringEnabledState = item.isRecurring
    }

    fun clearReceiptProcessingState() {
        _receiptProcessingState.value = ReceiptProcessingState.Idle
    }

    fun clearDetectedTransactions() {
        _detectedTransactions.value = emptyList()
    }

    fun initializeForNewTransaction(defaultAccountId: Long) {
        clearDetectedTransactions()
        transactionTypeState = null
        transactionAmountState = 0.0
        transactionDescriptionState = ""
        transactionCategoryState = 0L
        transactionAccountState = defaultAccountId
        isRecurringEnabledState = false
        editingRecurringTransactionId = null
        transactionDateState = getTodayStartOfDay()
    }

    fun resetFormForNewTransaction() {
        transactionAmountState = 0.0
        transactionDescriptionState = ""
        transactionCategoryState = 0L
        isRecurringEnabledState = false
        transactionDateState = getTodayStartOfDay()
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
            categoryId = categoryId,
            date = transactionDateState.takeIf { it != 0L },
            transactionType = transactionTypeState,
            accountId = transactionAccountState.takeIf { it != 0L },
            isRecurring = isRecurringEnabledState
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
        val date = transactionDateState
        val items = _detectedTransactions.value.toList()
        if (items.isEmpty()) {
            viewModelScope.launch { onSuccess(0) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val defaultOtherLabel = appContext.getString(R.string.category_default_other)
            for ((index, item) in items.withIndex()) {
                val type = item.transactionType ?: transactionTypeState ?: TransactionType.Gasto
                val itemDate = item.date ?: date
                val itemAccountId = item.accountId ?: transactionAccountState
                val categories = getCategoriesByTypeUseCase(type).firstOrNull().orEmpty()
                val category = CategoryNameMatcher.resolve(
                    suggestedName = item.categoryName,
                    categories = categories,
                    defaultOtherLabel = defaultOtherLabel
                ) ?: getCategoryByNameUseCase(item.categoryName).firstOrNull()
                val saveResult = saveTransactionInternal(
                    type = type,
                    amount = item.amount,
                    description = item.description,
                    categoryName = item.categoryName,
                    category = category,
                    date = itemDate,
                    iconName = null,
                    accountId = itemAccountId,
                    isRecurringEnabled = item.isRecurring
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
            analyticsManager.logTransactionsBatchSaved(items.size)
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
        updateRecurringDefault: Boolean? = null,
        onError: suspend (String) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val type = transactionTypeState ?: return@launch
            val amount = transactionAmountState
            val description = transactionDescriptionState.trim()
            val date = transactionDateState
            val trimmedCategoryName = categoryName.trim()
            val recurringActive = if (id != 0L && editingRecurringTransactionId != null) {
                isRecurringEnabledState
            } else {
                null
            }

            val saveResult = saveTransactionInternal(
                id = id,
                type = type,
                amount = amount,
                description = description,
                categoryName = trimmedCategoryName,
                category = category,
                date = date,
                iconName = iconName,
                accountId = transactionAccountState,
                isRecurringEnabled = id == 0L && isRecurringEnabledState,
                updateRecurringDefault = updateRecurringDefault,
                recurringActive = recurringActive
            )
            if (saveResult.isFailure) {
                val message = saveResult.exceptionOrNull()?.message.orEmpty()
                withContext(Dispatchers.Main) {
                    onError(if (message.isBlank()) appContext.getString(R.string.error_unknown) else message)
                }
                return@launch
            }
            analyticsManager.logTransactionSaved(type = type, isEdit = id != 0L)
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
        iconName: String?,
        accountId: Long,
        isRecurringEnabled: Boolean = false,
        updateRecurringDefault: Boolean? = null,
        recurringActive: Boolean? = null
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
            accountId = accountId,
            isRecurringEnabled = isRecurringEnabled,
            updateRecurringDefault = updateRecurringDefault,
            recurringActive = recurringActive,
            onError = { message -> errorMessage = message },
            onSuccess = { }
        )
        return errorMessage?.let { Result.failure(IllegalStateException(it)) } ?: Result.success(Unit)
    }
}
