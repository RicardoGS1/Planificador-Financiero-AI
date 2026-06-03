package com.virtualworld.easyexpensecontrol.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import android.provider.OpenableColumns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.ads.AiRewardedAdHelper
import com.virtualworld.easyexpensecontrol.audio.AudioRecorder
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.CategoryNameMatcher
import com.virtualworld.easyexpensecontrol.core.util.ImportedFileType
import com.virtualworld.easyexpensecontrol.core.util.CurrencyFormatter
import com.virtualworld.easyexpensecontrol.core.util.convertTimestampToString
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.data.model.Category
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.contracts.TakePictureWithUriGrants
import com.virtualworld.easyexpensecontrol.ui.components.AiAccessRewardedDialog
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.ExcelImportDateRangeDialog
import com.virtualworld.easyexpensecontrol.ui.components.GeminiAnalysisLoadingOverlay
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import com.virtualworld.easyexpensecontrol.ui.components.IconPickerDialog
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme
import com.virtualworld.easyexpensecontrol.ui.components.AccountSelectorChips
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.AiAnalysisSource
import com.virtualworld.easyexpensecontrol.viewmodel.ReceiptProcessingState
import com.virtualworld.easyexpensecontrol.viewmodel.DetectedTransactionItem
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale

private const val MAX_IMPORT_FILE_BYTES = 15 * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEditDetailTransactionView(
    id: Long,
    transactionViewModel: TransactionViewModel,
    categoryViewModel: CategoryViewModel,
    accountViewModel: AccountViewModel,
    navController: NavController
) {
    val accounts by accountViewModel.visibleAccounts.collectAsState(initial = emptyList())
    val receiptState by transactionViewModel.receiptProcessingState.collectAsState()
    val detectedTransactions by transactionViewModel.detectedTransactions.collectAsState()

    LaunchedEffect(accounts, transactionViewModel.transactionAccountState) {
        if (accounts.isEmpty()) return@LaunchedEffect
        if (accounts.none { it.id == transactionViewModel.transactionAccountState }) {
            transactionViewModel.transactionAccountState = accounts.first().id
        }
    }

    if (id != 0L) {
        val transaction = transactionViewModel.getTransactionById(id).collectAsState(
            initial = Transaction(0L, TransactionType.Ingreso, 0.0, 0L, 0L, "")
        )
        transaction.value.let {
            transactionViewModel.transactionTypeState = it.type
            transactionViewModel.transactionAmountState = it.amount
            transactionViewModel.transactionDescriptionState = it.description
            transactionViewModel.transactionCategoryState = it.category
            transactionViewModel.transactionDateState = it.date
            transactionViewModel.transactionAccountState = it.accountId
        }

        val category = categoryViewModel.getCategoryById(transactionViewModel.transactionCategoryState)
            .collectAsState(initial = null)
        LaunchedEffect(category.value) {
            category.value?.let {
                categoryViewModel.categoryNameState = it.name
                categoryViewModel.categoryTypeState = it.type
            }
        }
    } else {
        LaunchedEffect(id) {
            transactionViewModel.clearDetectedTransactions()
            transactionViewModel.transactionTypeState = null
            transactionViewModel.transactionAmountState = 0.0
            transactionViewModel.transactionDescriptionState = ""
            transactionViewModel.transactionCategoryState = 0L
            transactionViewModel.transactionAccountState = accounts.firstOrNull()?.id ?: 1L
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            transactionViewModel.transactionDateState = todayStart
        }
    }

    AddEditDetailTransactionContent(
        id = id,
        transactionTypeState = transactionViewModel.transactionTypeState,
        transactionAmountState = transactionViewModel.transactionAmountState,
        transactionDescriptionState = transactionViewModel.transactionDescriptionState,
        transactionCategoryState = transactionViewModel.transactionCategoryState,
        transactionDateState = transactionViewModel.transactionDateState,
        transactionAccountState = transactionViewModel.transactionAccountState,
        receiptState = receiptState,
        detectedTransactions = detectedTransactions,
        categoryNameState = categoryViewModel.categoryNameState,
        accounts = accounts,
        onTransactionTypeChanged = { transactionViewModel.transactionTypeState = it },
        onTransactionAmountChanged = { transactionViewModel.transactionAmountState = it },
        onTransactionDescriptionChanged = { transactionViewModel.transactionDescriptionState = it },
        onTransactionAccountChanged = { transactionViewModel.transactionAccountState = it },
        onTransactionDateChanged = transactionViewModel::onTransactionDateChanged,
        onCategoryNameChanged = { categoryViewModel.categoryNameState = it },
        processReceiptImage = transactionViewModel::processReceiptImage,
        processAudio = transactionViewModel::processAudio,
        processImportedFile = transactionViewModel::processImportedFile,
        clearReceiptProcessingState = transactionViewModel::clearReceiptProcessingState,
        addOrUpdateDetectedTransaction = transactionViewModel::addOrUpdateDetectedTransaction,
        commitDetectedTransactions = { onError, onSuccess ->
            transactionViewModel.commitDetectedTransactions(onError, onSuccess)
        },
        clearDetectedTransactions = transactionViewModel::clearDetectedTransactions,
        loadDetectedTransaction = transactionViewModel::loadDetectedTransaction,
        removeDetectedTransactionAt = transactionViewModel::removeDetectedTransactionAt,
        resetFormForNewTransaction = transactionViewModel::resetFormForNewTransaction,
        saveTransaction = { tid, catName, cat, icon, onError, onSuccess ->
            transactionViewModel.saveTransaction(tid, catName, cat, icon, onError, onSuccess)
        },
        getCategoryById = categoryViewModel::getCategoryById,
        getCategoriesByType = categoryViewModel::getCategoriesByType,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEditDetailTransactionContent(
    id: Long,
    transactionTypeState: TransactionType?,
    transactionAmountState: Double,
    transactionDescriptionState: String,
    transactionCategoryState: Long,
    transactionDateState: Long,
    transactionAccountState: Long,
    receiptState: ReceiptProcessingState,
    detectedTransactions: List<DetectedTransactionItem>,
    categoryNameState: String,
    accounts: List<Account>,
    onTransactionTypeChanged: (TransactionType) -> Unit,
    onTransactionAmountChanged: (Double) -> Unit,
    onTransactionDescriptionChanged: (String) -> Unit,
    onTransactionAccountChanged: (Long) -> Unit,
    onTransactionDateChanged: (Long) -> Unit,
    onCategoryNameChanged: (String) -> Unit,
    processReceiptImage: (ByteArray) -> Unit,
    processAudio: (ByteArray, TransactionType?) -> Unit,
    processImportedFile: (ByteArray, ImportedFileType, Long, Long) -> Unit,
    clearReceiptProcessingState: () -> Unit,
    addOrUpdateDetectedTransaction: (categoryName: String, categoryId: Long, updateIndex: Int?, onDone: () -> Unit) -> Unit,
    commitDetectedTransactions: (onError: suspend (String) -> Unit, onSuccess: suspend (Int) -> Unit) -> Unit,
    clearDetectedTransactions: () -> Unit,
    loadDetectedTransaction: (DetectedTransactionItem) -> Unit,
    removeDetectedTransactionAt: (Int) -> Boolean,
    resetFormForNewTransaction: () -> Unit,
    saveTransaction: (id: Long, categoryName: String, category: Category?, iconName: String?, onError: suspend (String) -> Unit, onSuccess: suspend () -> Unit) -> Unit,
    getCategoryById: (Long) -> kotlinx.coroutines.flow.Flow<Category?>,
    getCategoriesByType: (TransactionType) -> kotlinx.coroutines.flow.Flow<List<Category>>,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var categoryName by remember { mutableStateOf(categoryNameState) }
    LaunchedEffect(categoryNameState) { categoryName = categoryNameState }

    var selectedIconKey by remember { mutableStateOf<String?>(null) }
    val categoryFlowValue = getCategoryById(transactionCategoryState).collectAsState(initial = null)
    LaunchedEffect(categoryFlowValue.value) {
        categoryFlowValue.value?.let {
            selectedIconKey = it.iconName
        }
    }

    var showIconPicker by remember { mutableStateOf(false) }
    var isCategoryFocused by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }

    LaunchedEffect(transactionAmountState) {
        val modelAmount = transactionAmountState
        val textAmount = amountText.toDoubleOrNull() ?: 0.0
        if (modelAmount != textAmount) {
            amountText = if (modelAmount > 0) {
                if (modelAmount == modelAmount.toLong().toDouble())
                    modelAmount.toLong().toString()
                else modelAmount.toString()
            } else ""
        }
    }

    var isLoading by remember { mutableStateOf(false) }
    var isFinishing by remember { mutableStateOf(false) }
    var datePickerExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = TakePictureWithUriGrants()
    ) { success ->
        if (success && tempPhotoUri != null) {
            context.contentResolver.openInputStream(tempPhotoUri!!)?.use { it.readBytes() }?.let { bytes ->
                processReceiptImage(bytes)
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tempPhotoUri?.let { uri -> takePictureLauncher.launch(uri) }
        }
    }

    val audioRecorder = remember { AudioRecorder(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    DisposableEffect(audioRecorder) {
        onDispose { audioRecorder.cancel() }
    }

    fun startRecordingAudio() {
        try {
            audioRecorder.start()
            isRecordingAudio = true
        } catch (e: Exception) {
            isRecordingAudio = false
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_audio_record))
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        val bytes = audioRecorder.stopAndRead()
        isRecordingAudio = false
        if (bytes != null && bytes.isNotEmpty()) {
            processAudio(bytes, transactionTypeState)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_audio_record))
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecordingAudio()
    }

    val isAddMode = id == 0L
    var selectedDetectedIndex by remember { mutableStateOf<Int?>(null) }
    var showManualForm by remember { mutableStateOf(true) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showAiAccessDialog by remember { mutableStateOf(false) }
    var pendingAiAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showExcelDateRangeDialog by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImportFileName by remember { mutableStateOf("") }
    var pendingImportFileType by remember { mutableStateOf<ImportedFileType?>(null) }

    fun resolveDisplayFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                return cursor.getString(nameIndex).orEmpty()
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }

    fun detectImportFileType(uri: Uri, fileName: String): ImportedFileType? {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
            return ImportedFileType.PDF
        }
        if (mime.contains("spreadsheetml") || mime.contains("openxmlformats") ||
            fileName.endsWith(".xlsx", ignoreCase = true)
        ) {
            return ImportedFileType.XLSX
        }
        return null
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = resolveDisplayFileName(uri)
        val fileType = detectImportFileType(uri, fileName)
        if (fileType == null) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_spreadsheet_invalid_format))
            }
            return@rememberLauncherForActivityResult
        }
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
        if (bytes == null || bytes.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_spreadsheet_empty))
            }
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > MAX_IMPORT_FILE_BYTES) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_import_file_too_large))
            }
            return@rememberLauncherForActivityResult
        }
        pendingImportBytes = bytes
        pendingImportFileName = fileName
        pendingImportFileType = fileType
        showExcelDateRangeDialog = true
    }

    LaunchedEffect(transactionTypeState) {
        AiRewardedAdHelper.preload(context)
    }

    LaunchedEffect(receiptState) {
        when (receiptState) {
            is ReceiptProcessingState.Success -> {
                val success = receiptState as ReceiptProcessingState.Success
                val count = success.transactionCount
                val newStartIndex = (detectedTransactions.size - count).coerceAtLeast(0)
                val firstDetected = detectedTransactions.getOrNull(newStartIndex)
                if (firstDetected != null) {
                    categoryName = firstDetected.categoryName
                    onCategoryNameChanged(categoryName)
                    selectedDetectedIndex = newStartIndex
                    showManualForm = true
                }
                val message = when (success.source) {
                    AiAnalysisSource.SPREADSHEET -> if (count == 1) {
                        context.getString(R.string.spreadsheet_analyzed)
                    } else {
                        context.getString(R.string.spreadsheet_analyzed_multiple, count)
                    }
                    AiAnalysisSource.AUDIO -> context.getString(R.string.audio_analyzed)
                    AiAnalysisSource.RECEIPT -> if (count == 1) {
                        context.getString(R.string.receipt_analyzed)
                    } else {
                        context.getString(R.string.receipt_analyzed_multiple, count)
                    }
                }
                snackbarHostState.showSnackbar(message)
                clearReceiptProcessingState()
            }
            is ReceiptProcessingState.Error -> {
                snackbarHostState.showSnackbar((receiptState as ReceiptProcessingState.Error).message)
                clearReceiptProcessingState()
            }
            else -> { }
        }
    }

    val initialDateMillis = transactionDateState.takeIf { it != 0L }
        ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis
    )
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { onTransactionDateChanged(it) }
    }
    LaunchedEffect(transactionDateState) {
        val modelDate = transactionDateState
        if (modelDate != 0L && datePickerState.selectedDateMillis != modelDate) {
            datePickerState.selectedDateMillis = modelDate
        }
    }

    val currentType = transactionTypeState
    val categoriesByType by (
        if (currentType != null) getCategoriesByType(currentType)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    ).collectAsState(initial = emptyList())
    val filteredCategories = remember(categoriesByType, categoryName) {
        categoriesByType.filter { CategoryNameMatcher.matchesFilter(it.name, categoryName) }
    }
    val resolvedCategory = remember(categoriesByType, categoryName) {
        categoriesByType.firstOrNull { it.name.equals(categoryName.trim(), ignoreCase = true) }
    }

    fun resetLocalCategoryFields() {
        categoryName = ""
        onCategoryNameChanged("")
        selectedIconKey = null
    }

    fun validateTransactionForm(): Boolean {
        when {
            transactionTypeState == null -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_type_required))
                }
                return false
            }
            transactionAmountState <= 0 -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_amount_gt_zero))
                }
                return false
            }
            transactionDescriptionState.isBlank() -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_description_empty))
                }
                return false
            }
            categoryName.isBlank() -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_category_empty))
                }
                return false
            }
            datePickerState.selectedDateMillis == null -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_date_required))
                }
                return false
            }
            accounts.isNotEmpty() && accounts.none { it.id == transactionAccountState } -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.err_account_required))
                }
                return false
            }
            else -> return true
        }
    }

    fun addCurrentFormToDetected(onDone: () -> Unit) {
        addOrUpdateDetectedTransaction(
            categoryName,
            resolvedCategory?.id ?: transactionCategoryState,
            selectedDetectedIndex,
            {
                selectedDetectedIndex = null
                resetLocalCategoryFields()
                onDone()
            }
        )
    }

    fun commitAllDetected() {
        isFinishing = true
        commitDetectedTransactions(
            { error ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (error.isBlank()) context.getString(R.string.error_unknown)
                        else context.getString(R.string.error_prefix, error)
                    )
                    isFinishing = false
                }
            },
            {
                isFinishing = false
                scope.launch {
                    navController.navigateUp()
                }
            }
        )
    }

    fun hasUnsavedFormData(): Boolean =
        transactionAmountState > 0 ||
            transactionDescriptionState.isNotBlank() ||
            categoryName.isNotBlank()

    fun hasUnsavedPreparedWork(): Boolean =
        isAddMode && (detectedTransactions.isNotEmpty() || hasUnsavedFormData())

    fun attemptNavigateBack() {
        if (hasUnsavedPreparedWork()) {
            showExitConfirmDialog = true
        } else {
            navController.navigateUp()
        }
    }

    fun savePreparedAndExit() {
        showExitConfirmDialog = false
        when {
            hasUnsavedFormData() -> {
                if (!validateTransactionForm()) return
                addCurrentFormToDetected { commitAllDetected() }
            }
            detectedTransactions.isNotEmpty() -> commitAllDetected()
            else -> navController.navigateUp()
        }
    }

    fun discardPreparedAndExit() {
        showExitConfirmDialog = false
        clearDetectedTransactions()
        navController.navigateUp()
    }

    fun onAmountInputChange(value: String) {
        val normalized = value.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        val sanitized = run {
            val firstDot = filtered.indexOf('.')
            if (firstDot == -1) filtered
            else filtered.substring(0, firstDot + 1) +
                filtered.substring(firstDot + 1).replace(".", "")
        }
        amountText = sanitized
        onTransactionAmountChanged(
            sanitized.toDoubleOrNull() ?: 0.0
        )
    }

    val isAnalyzingReceipt = receiptState is ReceiptProcessingState.Loading
    val isIngreso = currentType == TransactionType.Ingreso
    val showForm = !isAddMode || showManualForm || detectedTransactions.isNotEmpty()
    val accentColor = when (currentType) {
        TransactionType.Ingreso -> MaterialTheme.colorScheme.primary
        TransactionType.Gasto -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val onCameraClick: () -> Unit = onCameraClick@{
        if (isAnalyzingReceipt || isRecordingAudio) return@onCameraClick
        val openCamera = {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempPhotoUri = uri
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePictureLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        if (AiRewardedAdHelper.hasSessionAccess()) {
            openCamera()
        } else {
            pendingAiAction = openCamera
            showAiAccessDialog = true
        }
    }

    if (isAddMode) {
        BackHandler {
            if (showExitConfirmDialog) {
                showExitConfirmDialog = false
            } else {
                attemptNavigateBack()
            }
        }
    }

    fun deleteSelectedPreparedTransaction() {
        val index = selectedDetectedIndex ?: return
        if (removeDetectedTransactionAt(index)) {
            selectedDetectedIndex = null
            resetLocalCategoryFields()
            resetFormForNewTransaction()
            amountText = ""
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.prepared_transaction_removed)
                )
                scrollState.animateScrollTo(0)
            }
        }
    }

    fun prepareOrUpdateTransaction() {
        if (!validateTransactionForm()) return
        if (isAddMode) {
            addCurrentFormToDetected {
                scope.launch {
                    launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.transaction_added_continue)
                        )
                    }
                    scrollState.animateScrollTo(0)
                }
            }
        } else {
            isLoading = true
            saveTransaction(
                id,
                categoryName,
                resolvedCategory,
                selectedIconKey,
                { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (error.isBlank()) context.getString(R.string.error_unknown)
                            else context.getString(R.string.error_prefix, error)
                        )
                        isLoading = false
                    }
                },
                {
                    scope.launch {
                        navController.navigateUp()
                    }
                }
            )
        }
    }

    val onMicClick: () -> Unit = onMicClick@{
        if (isAnalyzingReceipt) return@onMicClick
        if (isRecordingAudio) {
            stopRecordingAndAnalyze()
        } else {
            val startMic = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecordingAudio()
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            if (AiRewardedAdHelper.hasSessionAccess()) {
                startMic()
            } else {
                pendingAiAction = startMic
                showAiAccessDialog = true
            }
        }
    }

    val onExcelClick: () -> Unit = onExcelClick@{
        if (isAnalyzingReceipt || isRecordingAudio) return@onExcelClick
        val openPicker = {
            importFileLauncher.launch(
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/pdf"
                )
            )
        }
        if (AiRewardedAdHelper.hasSessionAccess()) {
            openPicker()
        } else {
            pendingAiAction = openPicker
            showAiAccessDialog = true
        }
    }

    val excelRangeDefaults = remember {
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = (end.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        start.timeInMillis to end.timeInMillis
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = !isAddMode || detectedTransactions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TransactionBottomBar(
                    isAddMode = isAddMode,
                    isLoading = isLoading,
                    isFinishing = isFinishing,
                    detectedCount = detectedTransactions.size,
                    isEditingPreparedTransaction = selectedDetectedIndex != null,
                    onSave = { prepareOrUpdateTransaction() },
                    showPrimaryButton = !isAddMode,
                    onFinish = {
                        val hasFormData = transactionAmountState > 0 ||
                            transactionDescriptionState.isNotBlank() ||
                            categoryName.isNotBlank()
                        when {
                            hasFormData -> {
                                if (!validateTransactionForm()) return@TransactionBottomBar
                                addCurrentFormToDetected { commitAllDetected() }
                            }
                            detectedTransactions.isEmpty() -> navController.navigateUp()
                            else -> commitAllDetected()
                        }
                    },
                    accentColor = accentColor,
                    isEditMode = !isAddMode
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScreenHeader(
                    title = if (!isAddMode) stringResource(id = R.string.update_transaction)
                    else stringResource(id = R.string.add_transaction),
                    showBackArrow = true,
                    onBackClick = { attemptNavigateBack() }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isAddMode && detectedTransactions.isNotEmpty()) {
                        DetectedTransactionsList(
                            transactions = detectedTransactions,
                            selectedIndex = selectedDetectedIndex,
                            onItemClick = { index, item ->
                                loadDetectedTransaction(item)
                                categoryName = item.categoryName
                                onCategoryNameChanged(item.categoryName)
                                selectedDetectedIndex = index
                            }
                        )
                    }

                    if (isAddMode) {
                        SectionTitle(text = stringResource(R.string.quick_actions))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (currentType != TransactionType.Ingreso) {
                                    AiQuickActionCard(
                                        icon = Icons.Default.CameraAlt,
                                        label = if (isAnalyzingReceipt) {
                                            stringResource(R.string.analyzing)
                                        } else {
                                            stringResource(R.string.take_photo)
                                        },
                                        isActive = isAnalyzingReceipt,
                                        isEnabled = !isAnalyzingReceipt && !isRecordingAudio,
                                        onClick = onCameraClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                AiQuickActionCard(
                                    icon = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                                    label = when {
                                        isAnalyzingReceipt -> stringResource(R.string.analyzing)
                                        isRecordingAudio -> stringResource(R.string.recording_in_progress)
                                        else -> stringResource(R.string.record_audio)
                                    },
                                    isActive = isRecordingAudio || isAnalyzingReceipt,
                                    isEnabled = !isAnalyzingReceipt,
                                    onClick = onMicClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AiQuickActionCard(
                                    icon = Icons.Default.TableChart,
                                    label = if (isAnalyzingReceipt) {
                                        stringResource(R.string.analyzing)
                                    } else {
                                        stringResource(R.string.import_excel)
                                    },
                                    isActive = isAnalyzingReceipt,
                                    isEnabled = !isAnalyzingReceipt && !isRecordingAudio,
                                    onClick = onExcelClick,
                                    modifier = Modifier.weight(1f)
                                )
                                AiQuickActionCard(
                                    icon = Icons.Default.Edit,
                                    label = stringResource(R.string.manual_entry),
                                    isActive = showManualForm,
                                    isEnabled = !isAnalyzingReceipt && !isRecordingAudio,
                                    onClick = { showManualForm = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (!showForm) {
                            AiOrManualHintCard()
                        }
                    }

                    AnimatedVisibility(
                        visible = showForm,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SectionTitle(text = stringResource(R.string.transaction_type))
                            TransactionTypeSelector(
                                selectedType = currentType,
                                onTypeChanged = onTransactionTypeChanged
                            )

                            if (currentType == null && isAddMode) {
                                TypeSelectionHintCard()
                            }

                            AnimatedVisibility(
                                visible = currentType != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HeroAmountCard(
                                amountText = amountText,
                                onAmountChange = ::onAmountInputChange,
                                accentColor = accentColor,
                                isIngreso = isIngreso
                            )

                            if (accounts.isNotEmpty()) {
                                SectionTitle(
                                    text = stringResource(R.string.account_label),
                                    icon = Icons.Default.AccountBalanceWallet
                                )
                                AccountSelectorChips(
                                    accounts = accounts,
                                    selectedAccountId = transactionAccountState,
                                    onAccountSelected = onTransactionAccountChanged,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            SectionTitle(
                                text = stringResource(R.string.transaction_details),
                                icon = Icons.Default.Description
                            )
                            FormSectionCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stringResource(R.string.description_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AppTextField(
                                        label = stringResource(R.string.hint_description),
                                        value = transactionDescriptionState,
                                        onValueChange = onTransactionDescriptionChanged,
                                        labelStyle = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.focusScrollIntoView()
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                CategoryFormSection(
                                    categoryName = categoryName,
                                    onCategoryNameChange = { value ->
                                        categoryName = value
                                        onCategoryNameChanged(value)
                                    },
                                    filteredCategories = filteredCategories,
                                    selectedIconKey = selectedIconKey,
                                    accentColor = accentColor,
                                    onIconPickerClick = { showIconPicker = true },
                                    onCategorySelected = { cat ->
                                        categoryName = cat.name
                                        onCategoryNameChanged(cat.name)
                                        selectedIconKey = cat.iconName
                                    },
                                    onFocusChanged = { isCategoryFocused = it }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { datePickerExpanded = !datePickerExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.date_label),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = datePickerState.selectedDateMillis?.let { convertTimestampToString(it) }
                                                    ?: stringResource(R.string.tap_pick_date),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (datePickerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (datePickerExpanded) stringResource(R.string.cd_hide_calendar)
                                        else stringResource(R.string.cd_change_date),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = datePickerExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    DatePicker(
                                        state = datePickerState,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = DatePickerDefaults.colors(
                                            selectedDayContainerColor = AccentBlue,
                                            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                                            todayDateBorderColor = AccentBlue,
                                            todayContentColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            if (isAddMode) {
                                if (selectedDetectedIndex != null) {
                                    PreparedTransactionEditActions(
                                        isLoading = isLoading,
                                        isFinishing = isFinishing,
                                        accentColor = accentColor,
                                        onFinishEditing = { prepareOrUpdateTransaction() },
                                        onDelete = { deleteSelectedPreparedTransaction() }
                                    )
                                } else {
                                    Button(
                                        onClick = { prepareOrUpdateTransaction() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        enabled = !isLoading && !isFinishing,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.prepare_transaction),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                    }

                    Spacer(
                        modifier = Modifier.height(
                            when {
                                isCategoryFocused && imeBottomPadding > 0.dp ->
                                    imeBottomPadding + 16.dp
                                isAddMode && detectedTransactions.isNotEmpty() -> 88.dp
                                else -> 8.dp
                            }
                        )
                    )
                }
            }

            if (showIconPicker) {
                IconPickerDialog(
                    selectedIconKey = selectedIconKey,
                    onIconSelected = { key -> selectedIconKey = key },
                    onDismiss = { showIconPicker = false }
                )
            }

            if (receiptState is ReceiptProcessingState.Loading) {
                GeminiAnalysisLoadingOverlay()
            }

            if (showExitConfirmDialog) {
                UnsavedPreparedTransactionsDialog(
                    transactions = detectedTransactions,
                    hasUnsavedFormData = hasUnsavedFormData(),
                    onDismiss = { showExitConfirmDialog = false },
                    onSave = { savePreparedAndExit() },
                    onDiscard = { discardPreparedAndExit() }
                )
            }

            if (showExcelDateRangeDialog && pendingImportBytes != null && pendingImportFileType != null) {
                ExcelImportDateRangeDialog(
                    fileName = pendingImportFileName,
                    initialStartMillis = excelRangeDefaults.first,
                    initialEndMillis = excelRangeDefaults.second,
                    onDismiss = {
                        showExcelDateRangeDialog = false
                        pendingImportBytes = null
                        pendingImportFileName = ""
                        pendingImportFileType = null
                    },
                    onAnalyze = { startMillis, endMillis ->
                        val bytes = pendingImportBytes ?: return@ExcelImportDateRangeDialog
                        val fileType = pendingImportFileType ?: return@ExcelImportDateRangeDialog
                        showExcelDateRangeDialog = false
                        pendingImportBytes = null
                        pendingImportFileName = ""
                        pendingImportFileType = null
                        processImportedFile(bytes, fileType, startMillis, endMillis)
                    }
                )
            }

            if (showAiAccessDialog) {
                AiAccessRewardedDialog(
                    context = context,
                    onDismiss = {
                        showAiAccessDialog = false
                        pendingAiAction = null
                    },
                    onShowAd = { activity ->
                        val action = pendingAiAction ?: return@AiAccessRewardedDialog
                        showAiAccessDialog = false
                        AiRewardedAdHelper.showForSessionAccess(
                            activity = activity,
                            onGranted = {
                                pendingAiAction = null
                                action()
                            },
                            onAdFailed = {
                                showAiAccessDialog = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.ai_access_ad_failed)
                                    )
                                }
                            },
                            onAdNotCompleted = {
                                showAiAccessDialog = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.ai_access_ad_not_completed)
                                    )
                                }
                            }
                        )
                    },
                    onLoadFailed = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.ai_access_ad_failed)
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryFormSection(
    categoryName: String,
    onCategoryNameChange: (String) -> Unit,
    filteredCategories: List<Category>,
    selectedIconKey: String?,
    accentColor: androidx.compose.ui.graphics.Color,
    onIconPickerClick: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    fun scrollSectionIntoView() {
        scope.launch {
            delay(300)
            bringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(isFocused, filteredCategories) {
        if (isFocused) {
            delay(300)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredCategories.isNotEmpty()) {
            Text(
                text = stringResource(R.string.existing_categories),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filteredCategories.forEach { cat ->
                    CategoryChip(
                        name = cat.name,
                        iconKey = cat.iconName,
                        isSelected = categoryName.equals(cat.name, ignoreCase = true),
                        accentColor = accentColor,
                        onClick = { onCategorySelected(cat) }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(
                        width = 2.dp,
                        color = accentColor.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onIconPickerClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIcons.getIcon(selectedIconKey),
                    contentDescription = stringResource(R.string.cd_change_icon),
                    modifier = Modifier.size(24.dp),
                    tint = accentColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AppTextField(
                    label = stringResource(R.string.hint_category),
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    labelStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.onFocusEvent { focusState ->
                        isFocused = focusState.isFocused
                        onFocusChanged(focusState.isFocused)
                        if (focusState.isFocused) {
                            scrollSectionIntoView()
                        }
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.tap_icon_to_change),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.focusScrollIntoView(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    delay(150)
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}

@Composable
private fun SectionTitle(
    text: String,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AiOrManualHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = stringResource(R.string.ai_or_manual_hint),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TypeSelectionHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            InputOptionHintRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.manual_entry),
                leadingNote = stringResource(R.string.select_type_to_continue),
                description = stringResource(R.string.input_option_manual_desc)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            InputOptionHintRow(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.record_audio),
                description = stringResource(R.string.input_option_audio_desc),
                example = stringResource(R.string.input_option_audio_example)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            InputOptionHintRow(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.take_photo),
                description = stringResource(R.string.input_option_scan_desc)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            InputOptionHintRow(
                icon = Icons.Default.TableChart,
                title = stringResource(R.string.import_excel),
                description = stringResource(R.string.input_option_excel_desc)
            )
        }
    }
}

@Composable
private fun InputOptionHintRow(
    icon: ImageVector,
    title: String,
    description: String,
    leadingNote: String? = null,
    example: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            leadingNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            example?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun HeroAmountCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    isIngreso: Boolean
) {
    val currencySymbol = CurrencyFormatter.symbol(LocalContext.current)
    val hintText = stringResource(R.string.hint_amount)
    val amountTextStyle = TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor
    )
    val placeholderStyle = amountTextStyle.copy(color = accentColor.copy(alpha = 0.35f))
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val hintWidthDp = remember(hintText, amountTextStyle, density) {
        with(density) {
            textMeasurer.measure(hintText, amountTextStyle).size.width.toDp()
        }
    }

    var amountFieldValue by remember {
        mutableStateOf(TextFieldValue(amountText, TextRange(amountText.length)))
    }

    LaunchedEffect(amountText) {
        if (amountFieldValue.text != amountText) {
            amountFieldValue = TextFieldValue(
                text = amountText,
                selection = TextRange(if (amountText.isEmpty()) 0 else amountText.length)
            )
        }
    }

    val isAmountEmpty = amountText.isEmpty()
    val displayText = if (isAmountEmpty) hintText else amountText
    val fieldWidthDp = remember(displayText, amountTextStyle, density, hintWidthDp) {
        maxOf(
            hintWidthDp,
            with(density) {
                textMeasurer.measure(displayText, amountTextStyle).size.width.toDp()
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.amount_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isIngreso || !isAmountEmpty) {
                    Text(
                        text = if (isIngreso) "+" else "−",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    BasicTextField(
                        value = if (isAmountEmpty) {
                            TextFieldValue("", TextRange(0))
                        } else {
                            amountFieldValue
                        },
                        onValueChange = { newValue ->
                            amountFieldValue = newValue
                            onAmountChange(newValue.text)
                        },
                        textStyle = amountTextStyle.copy(textAlign = TextAlign.Start),
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.width(fieldWidthDp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (isAmountEmpty) {
                                    Text(text = hintText, style = placeholderStyle)
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .width(fieldWidthDp)
                            .focusScrollIntoView()
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun AiQuickActionCard(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.5f)
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PreparedTransactionEditActions(
    isLoading: Boolean,
    isFinishing: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onFinishEditing: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = !isLoading && !isFinishing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text(
                text = stringResource(R.string.remove_prepared_transaction),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Button(
            onClick = onFinishEditing,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = stringResource(R.string.finish_editing_prepared),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TransactionBottomBar(
    isAddMode: Boolean,
    isLoading: Boolean,
    isFinishing: Boolean,
    detectedCount: Int,
    isEditingPreparedTransaction: Boolean,
    onSave: () -> Unit,
    onFinish: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    isEditMode: Boolean,
    showPrimaryButton: Boolean = true
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showPrimaryButton) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading && !isFinishing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = when {
                                isEditMode -> stringResource(id = R.string.update_transaction)
                                isEditingPreparedTransaction -> stringResource(R.string.finish_editing_prepared)
                                else -> stringResource(id = R.string.prepare_transaction)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isAddMode && detectedCount > 0) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading && !isFinishing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isFinishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAddCheck,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.finish_and_save),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = detectedCount.toString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsavedPreparedTransactionsDialog(
    transactions: List<DetectedTransactionItem>,
    hasUnsavedFormData: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val dialogScrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_without_saving_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (transactions.isNotEmpty()) {
                        stringResource(R.string.exit_without_saving_message, transactions.size)
                    } else {
                        stringResource(R.string.exit_without_saving_form_only_message)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transactions.isNotEmpty()) {
                    transactions.forEach { item ->
                        DetectedTransactionRow(
                            item = item,
                            isSelected = false
                        )
                    }
                }
                if (hasUnsavedFormData) {
                    Text(
                        text = stringResource(R.string.exit_unsaved_form_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save_and_exit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard_without_saving))
            }
        }
    )
}

@Composable
private fun DetectedTransactionsList(
    transactions: List<DetectedTransactionItem>,
    selectedIndex: Int?,
    onItemClick: (Int, DetectedTransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = containerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.prepared_transactions_session, transactions.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.tap_detected_to_edit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            transactions.forEachIndexed { index, item ->
                DetectedTransactionRow(
                    item = item,
                    isSelected = selectedIndex == index,
                    onClick = { onItemClick(index, item) }
                )
            }
        }
    }
}

@Composable
private fun DetectedTransactionRow(
    item: DetectedTransactionItem,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val rowShape = RoundedCornerShape(14.dp)
    val rowBorderColor = if (isSelected) {
        AccentBlue.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = rowShape,
        color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = rowBorderColor
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description.ifBlank { stringResource(R.string.no_description) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = CurrencyFormatter.formatSigned(
                    context,
                    item.amount,
                    item.transactionType == TransactionType.Ingreso
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (item.transactionType == TransactionType.Ingreso) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType?,
    onTypeChanged: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selectedType
            val typeColor = if (type == TransactionType.Ingreso) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) typeColor else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    )
                    .clickable { onTypeChanged(type) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (type == TransactionType.Ingreso) Icons.Default.TrendingUp
                        else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (type) {
                            TransactionType.Ingreso -> stringResource(R.string.transaction_type_income)
                            TransactionType.Gasto -> stringResource(R.string.transaction_type_expense)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    iconKey: String?,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon(iconKey),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionTypeDropdown(
    selectedType: TransactionType?,
    onTypeChanged: (TransactionType) -> Unit
) {
    TransactionTypeSelector(selectedType = selectedType, onTypeChanged = onTypeChanged)
}

@Preview(showBackground = true)
@Composable
fun AddEditDetailTransactionViewPreview() {
    val sampleAccounts = listOf(
        Account(1L, "Efectivo"),
        Account(2L, "Tarjeta de Crédito")
    )
    EasyExpenseControlTheme {
        AddEditDetailTransactionContent(
            id = 0L,
            transactionTypeState = TransactionType.Gasto,
            transactionAmountState = 50.0,
            transactionDescriptionState = "Compra semanal",
            transactionAccountState = 1L,
            transactionDateState = System.currentTimeMillis(),
            transactionCategoryState = 0L,
            receiptState = ReceiptProcessingState.Idle,
            detectedTransactions = emptyList(),
            categoryNameState = "Supermercado",
            accounts = sampleAccounts,
            onTransactionTypeChanged = {},
            onTransactionAmountChanged = {},
            onTransactionDescriptionChanged = {},
            onTransactionAccountChanged = {},
            onTransactionDateChanged = {},
            onCategoryNameChanged = {},
            processReceiptImage = {},
            processAudio = { _, _ -> },
            processImportedFile = { _, _, _, _ -> },
            clearReceiptProcessingState = {},
            addOrUpdateDetectedTransaction = { _, _, _, _ -> },
            commitDetectedTransactions = { _, _ -> },
            clearDetectedTransactions = {},
            loadDetectedTransaction = {},
            removeDetectedTransactionAt = { false },
            resetFormForNewTransaction = {},
            saveTransaction = { _, _, _, _, _, _ -> },
            getCategoryById = { kotlinx.coroutines.flow.flowOf(null) },
            getCategoriesByType = { kotlinx.coroutines.flow.flowOf(emptyList()) },
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetectedTransactionsListPreview() {
    val sampleTransactions = listOf(
        DetectedTransactionItem(amount = 25.50, description = "Supermercado", categoryName = "Comida"),
        DetectedTransactionItem(amount = 10.0, description = "Gasolina", categoryName = "Transporte"),
        DetectedTransactionItem(amount = 5.0, description = "Café", categoryName = "Entretenimiento")
    )
    EasyExpenseControlTheme {
        DetectedTransactionsList(
            transactions = sampleTransactions,
            selectedIndex = 1,
            onItemClick = { _, _ -> }
        )
    }
}
