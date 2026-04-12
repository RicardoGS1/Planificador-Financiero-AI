package com.virtualworld.easyexpensecontrol.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.ads.CameraInterstitialAdHelper
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.convertTimestampToString
import com.virtualworld.easyexpensecontrol.data.model.Transaction
import com.virtualworld.easyexpensecontrol.data.model.TransactionType
import com.virtualworld.easyexpensecontrol.ui.contracts.TakePictureWithUriGrants
import com.virtualworld.easyexpensecontrol.ui.components.AppTextField
import com.virtualworld.easyexpensecontrol.ui.components.CategoryIcons
import com.virtualworld.easyexpensecontrol.ui.components.IconPickerDialog
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.ReceiptProcessingState
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedIconKey by remember { mutableStateOf<String?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = TakePictureWithUriGrants()
    ) { success ->
        if (success && tempPhotoUri != null) {
            context.contentResolver.openInputStream(tempPhotoUri!!)?.use { it.readBytes() }?.let { bytes ->
                transactionViewModel.processReceiptImage(bytes)
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

    val receiptState by transactionViewModel.receiptProcessingState.collectAsState()
    LaunchedEffect(receiptState) {
        when (receiptState) {
            is ReceiptProcessingState.Success -> {
                categoryName = (receiptState as ReceiptProcessingState.Success).categoryNameForUi
                categoryViewModel.categoryNameState = categoryName
                snackbarHostState.showSnackbar(context.getString(R.string.receipt_analyzed))
                transactionViewModel.clearReceiptProcessingState()
            }
            is ReceiptProcessingState.Error -> {
                snackbarHostState.showSnackbar((receiptState as ReceiptProcessingState.Error).message)
                transactionViewModel.clearReceiptProcessingState()
            }
            else -> { }
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
        }

        val category = categoryViewModel.getCategoryById(transactionViewModel.transactionCategoryState)
            .collectAsState(initial = null)
        LaunchedEffect(category.value) {
            category.value?.let {
                categoryName = it.name
                selectedIconKey = it.iconName
                categoryViewModel.categoryNameState = it.name
                categoryViewModel.categoryTypeState = it.type
            }
        }
    } else {
        // Solo inicializar estado al entrar en pantalla "añadir", no en cada recomposición,
        // para no sobrescribir los valores que rellenó el análisis del comprobante (Gemini).
        LaunchedEffect(id) {
            transactionViewModel.transactionTypeState = TransactionType.Ingreso
            transactionViewModel.transactionAmountState = 0.0
            transactionViewModel.transactionDescriptionState = ""
            transactionViewModel.transactionCategoryState = 0L
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            transactionViewModel.transactionDateState = todayStart
        }
    }

    val initialDateMillis = transactionViewModel.transactionDateState.takeIf { it != 0L }
        ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayedMonthMillis = initialDateMillis
    )
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { transactionViewModel.onTransactionDateChanged(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = if (id != 0L) stringResource(id = R.string.update_transaction)
                else stringResource(id = R.string.add_transaction),
                showBackArrow = true,
                onBackClick = { navController.navigateUp() }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tipo de transacción — selector integrado
                Text(
                    text = stringResource(R.string.transaction_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TransactionTypeSelector(
                    selectedType = transactionViewModel.transactionTypeState,
                    onTypeChanged = transactionViewModel::onTransactionTypeChanged
                )

                if (transactionViewModel.transactionTypeState == TransactionType.Gasto) {
                    val isAnalyzingReceipt = receiptState is ReceiptProcessingState.Loading
                    FilledTonalButton(
                        onClick = {
                            if (!isAnalyzingReceipt) {
                                val activity = context as? Activity ?: return@FilledTonalButton
                                CameraInterstitialAdHelper.showThenContinue(activity) {
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
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isAnalyzingReceipt
                    ) {
                        if (isAnalyzingReceipt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isAnalyzingReceipt) stringResource(R.string.analyzing)
                            else stringResource(R.string.take_photo)
                        )
                    }
                }

                // Importe
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.amount_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            label = stringResource(R.string.hint_amount),
                            value = if (transactionViewModel.transactionAmountState > 0)
                                transactionViewModel.transactionAmountState.toString() else "",
                            onValueChange = { value ->
                                transactionViewModel.onTransactionAmountChanged(value.toDoubleOrNull() ?: 0.0)
                            },
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                // Descripción
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.description_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            label = stringResource(R.string.hint_description),
                            value = transactionViewModel.transactionDescriptionState,
                            onValueChange = transactionViewModel::onTransactionDescriptionChanged
                        )
                    }
                }

                // Categoría: escribir nueva o elegir existente
                val categoriesByType by categoryViewModel.getCategoriesByType(transactionViewModel.transactionTypeState)
                    .collectAsState(initial = emptyList())
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.category_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { showIconPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.getIcon(selectedIconKey),
                                    contentDescription = stringResource(R.string.cd_change_icon),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                AppTextField(
                                    label = stringResource(R.string.hint_category),
                                    value = categoryName,
                                    onValueChange = { categoryName = it }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.tap_icon_to_change),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (categoriesByType.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.existing_categories),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categoriesByType.forEach { cat ->
                                    CategoryChip(
                                        name = cat.name,
                                        iconKey = cat.iconName,
                                        isSelected = categoryName.equals(cat.name, ignoreCase = true),
                                        onClick = {
                                            categoryName = cat.name
                                            selectedIconKey = cat.iconName
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showIconPicker) {
                    IconPickerDialog(
                        selectedIconKey = selectedIconKey,
                        onIconSelected = { key -> selectedIconKey = key },
                        onDismiss = { showIconPicker = false }
                    )
                }

                val category by categoryViewModel.getCategoryByName(categoryName).collectAsState(initial = null)

                // Fecha — compacto hasta que el usuario toque para editar
                var datePickerExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerExpanded = !datePickerExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
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
                                        fontWeight = FontWeight.Medium
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
                        if (datePickerExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                }

                var isLoading by remember { mutableStateOf(false) }

                FilledTonalButton(
                    onClick = {
                        when {
                            transactionViewModel.transactionAmountState <= 0 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_amount_gt_zero))
                                }
                            }
                            transactionViewModel.transactionDescriptionState.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_description_empty))
                                }
                            }
                            categoryName.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_category_empty))
                                }
                            }
                            datePickerState.selectedDateMillis == null -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.err_date_required))
                                }
                            }
                            else -> {
                                isLoading = true
                                transactionViewModel.saveTransaction(
                                    id = id,
                                    categoryName = categoryName,
                                    category = category,
                                    iconName = selectedIconKey,
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (error.isBlank()) context.getString(R.string.error_unknown)
                                                else context.getString(R.string.error_prefix, error)
                                            )
                                            isLoading = false
                                        }
                                    },
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.success_operation))
                                            navController.navigateUp()
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Text(
                            text = if (id != 0L) stringResource(id = R.string.update_transaction)
                            else stringResource(id = R.string.add_transaction),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selectedType
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeChanged(type) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) AccentBlue
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (type == TransactionType.Ingreso) Icons.Default.TrendingUp
                        else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = when (type) {
                            TransactionType.Ingreso -> stringResource(R.string.transaction_type_income)
                            TransactionType.Gasto -> stringResource(R.string.transaction_type_expense)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentBlue
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit
) {
    TransactionTypeSelector(selectedType = selectedType, onTypeChanged = onTypeChanged)
}
