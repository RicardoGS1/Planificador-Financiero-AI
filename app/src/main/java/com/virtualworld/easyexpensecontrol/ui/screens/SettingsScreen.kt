package com.virtualworld.easyexpensecontrol.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.util.CurrencyHelper
import com.virtualworld.easyexpensecontrol.core.util.LocaleHelper
import com.virtualworld.easyexpensecontrol.data.model.Account
import com.virtualworld.easyexpensecontrol.ui.components.ScreenHeader
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue
import com.virtualworld.easyexpensecontrol.ui.theme.AccentPink
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
    accountViewModel: AccountViewModel
) {
    val context = LocalContext.current
    val accounts by accountViewModel.accounts.collectAsState(initial = emptyList())
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAccountsDialog by remember { mutableStateOf(false) }
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountHidden by remember { mutableStateOf(false) }
    var editAccountError by remember { mutableStateOf<String?>(null) }
    val currentCurrencyCode = remember { CurrencyHelper.getSavedCurrencyCode(context) }
    val currentCurrencyInfo = remember(currentCurrencyCode) {
        CurrencyHelper.currencyInfo(context, currentCurrencyCode)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScreenHeader(
                title = stringResource(R.string.screen_settings),
                showBackArrow = true,
                onBackClick = { navController.popBackStack() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_section_general),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            SettingsItemCard(
                icon = Icons.Filled.Language,
                iconBackground = Brush.linearGradient(listOf(AccentBlue, Color(0xFF6BB1FF))),
                title = stringResource(R.string.settings_language),
                subtitle = currentLanguageDisplay(LocaleHelper.getSavedLanguageTag(context)),
                onClick = { showLanguageDialog = true }
            )

            SettingsItemCard(
                icon = Icons.Filled.AttachMoney,
                iconBackground = Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))),
                title = stringResource(R.string.settings_currency),
                subtitle = stringResource(
                    R.string.settings_currency_subtitle,
                    currentCurrencyInfo.displayName,
                    currentCurrencyInfo.code,
                    currentCurrencyInfo.symbol
                ),
                onClick = { showCurrencyDialog = true }
            )

            SettingsItemCard(
                icon = Icons.Filled.AccountBalance,
                iconBackground = Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5))),
                title = stringResource(R.string.settings_accounts),
                subtitle = buildAccountsSettingsSubtitle(accounts),
                onClick = { showAccountsDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val shareSubject = stringResource(R.string.share_subject)
            val shareText = stringResource(R.string.share_text)
            val shareChooserTitle = stringResource(R.string.settings_share)
            SettingsItemCard(
                icon = Icons.Filled.Share,
                iconBackground = Brush.linearGradient(listOf(AccentPink, Color(0xFFFF8AB1))),
                title = stringResource(R.string.settings_share),
                subtitle = stringResource(R.string.settings_share_subtitle),
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, shareChooserTitle))
                }
            )

            SettingsItemCard(
                icon = Icons.Filled.Star,
                iconBackground = Brush.linearGradient(
                    listOf(Color(0xFFFFB347), Color(0xFFFF9800))
                ),
                title = stringResource(R.string.settings_rate_app),
                subtitle = stringResource(R.string.settings_rate_app_subtitle),
                onClick = { openPlayStoreListing(context) }
            )
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentTag = LocaleHelper.getSavedLanguageTag(context),
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { tag ->
                showLanguageDialog = false
                if (tag != LocaleHelper.getSavedLanguageTag(context)) {
                    LocaleHelper.setLanguageTag(context, tag)
                    (context as? Activity)?.recreate()
                }
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            currentCode = CurrencyHelper.getSavedCurrencyCode(context),
            onDismiss = { showCurrencyDialog = false },
            onCurrencySelected = { code ->
                showCurrencyDialog = false
                if (code != CurrencyHelper.getSavedCurrencyCode(context)) {
                    CurrencyHelper.setCurrencyCode(context, code)
                    (context as? Activity)?.recreate()
                }
            }
        )
    }

    if (showAccountsDialog) {
        AccountsPickerDialog(
            accounts = accounts,
            onDismiss = { showAccountsDialog = false },
            onAccountSelected = { account ->
                showAccountsDialog = false
                editingAccount = account
                editAccountName = account.name
                editAccountHidden = account.isHidden
                editAccountError = null
                showEditAccountDialog = true
            }
        )
    }

    if (showEditAccountDialog && editingAccount != null) {
        EditAccountDialog(
            accountName = editAccountName,
            isHidden = editAccountHidden,
            error = editAccountError,
            onAccountNameChange = {
                editAccountName = it
                editAccountError = null
            },
            onHiddenChange = { editAccountHidden = it },
            onDismiss = {
                showEditAccountDialog = false
                editingAccount = null
                editAccountName = ""
                editAccountHidden = false
                editAccountError = null
            },
            onConfirm = {
                val account = editingAccount ?: return@EditAccountDialog
                if (editAccountName.isBlank()) {
                    editAccountError = context.getString(R.string.err_account_name_empty)
                    return@EditAccountDialog
                }
                accountViewModel.updateAccount(
                    account = account,
                    name = editAccountName,
                    isHidden = editAccountHidden,
                    onError = { error ->
                        editAccountError = error.ifBlank {
                            context.getString(R.string.error_unknown)
                        }
                    },
                    onSuccess = {
                        showEditAccountDialog = false
                        editingAccount = null
                        editAccountName = ""
                        editAccountHidden = false
                        editAccountError = null
                    }
                )
            }
        )
    }
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    iconBackground: Brush,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_language))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LocaleHelper.SUPPORTED_LANGUAGE_TAGS.forEach { tag ->
                    val isSelected = tag == currentTag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageSelected(tag) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = languageFlag(tag),
                            modifier = Modifier.width(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = languageDisplayName(tag),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CurrencyPickerDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit
) {
    val context = LocalContext.current
    val allCurrencies = remember(context) { CurrencyHelper.getAllCurrencies(context) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(allCurrencies, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            allCurrencies
        } else {
            allCurrencies.filter { currency ->
                currency.code.contains(query, ignoreCase = true) ||
                    currency.displayName.contains(query, ignoreCase = true) ||
                    currency.symbol.contains(query, ignoreCase = true) ||
                    currency.pickerLabel.contains(query, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_currency))
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.settings_currency_search_hint)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(filteredCurrencies, key = { it.code }) { currency ->
                        val isSelected = currency.code == currentCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCurrencySelected(currency.code) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currency.pickerLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AccountsPickerDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_accounts))
            }
        },
        text = {
            if (accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_accounts_subtitle_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAccountSelected(account) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (account.isHidden) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (account.isHidden) FontWeight.Normal else FontWeight.Medium
                                )
                                if (account.isHidden) {
                                    Text(
                                        text = stringResource(R.string.account_hidden_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (account.isHidden) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = stringResource(R.string.account_hidden_label),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(18.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun EditAccountDialog(
    accountName: String,
    isHidden: Boolean,
    error: String?,
    onAccountNameChange: (String) -> Unit,
    onHiddenChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_account_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = onAccountNameChange,
                    label = { Text(stringResource(R.string.account_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.account_show_in_app),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.account_show_in_app_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = !isHidden,
                        onCheckedChange = { onHiddenChange(!it) }
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun buildAccountsSettingsSubtitle(accounts: List<Account>): String {
    if (accounts.isEmpty()) {
        return stringResource(R.string.settings_accounts_subtitle_empty)
    }
    val visibleNames = accounts.filterNot { it.isHidden }.joinToString(", ") { it.name }
    val hiddenCount = accounts.count { it.isHidden }
    return when {
        hiddenCount == 0 -> visibleNames
        visibleNames.isBlank() -> stringResource(R.string.settings_accounts_all_hidden, hiddenCount)
        else -> stringResource(R.string.settings_accounts_subtitle_with_hidden, visibleNames, hiddenCount)
    }
}

@Composable
private fun currentLanguageDisplay(tag: String): String {
    return languageDisplayName(tag)
}

@Composable
private fun languageDisplayName(tag: String): String = when (tag) {
    "" -> stringResource(R.string.language_system)
    "en" -> stringResource(R.string.language_en)
    "es" -> stringResource(R.string.language_es)
    "de" -> stringResource(R.string.language_de)
    "hi" -> stringResource(R.string.language_hi)
    "ru" -> stringResource(R.string.language_ru)
    "pt-BR" -> stringResource(R.string.language_pt)
    "fr" -> stringResource(R.string.language_fr)
    "id" -> stringResource(R.string.language_id)
    "it" -> stringResource(R.string.language_it)
    "ar" -> stringResource(R.string.language_ar)
    "tr" -> stringResource(R.string.language_tr)
    else -> tag
}

private fun openPlayStoreListing(context: Context) {
    val id = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$id")
    )
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$id")
            )
        )
    }
}

private fun languageFlag(tag: String): String = when (tag) {
    "" -> "\uD83C\uDF10"
    "en" -> "\uD83C\uDDEC\uD83C\uDDE7"
    "es" -> "\uD83C\uDDEA\uD83C\uDDF8"
    "de" -> "\uD83C\uDDE9\uD83C\uDDEA"
    "hi" -> "\uD83C\uDDEE\uD83C\uDDF3"
    "ru" -> "\uD83C\uDDF7\uD83C\uDDFA"
    "pt-BR" -> "\uD83C\uDDE7\uD83C\uDDF7"
    "fr" -> "\uD83C\uDDEB\uD83C\uDDF7"
    "id" -> "\uD83C\uDDEE\uD83C\uDDE9"
    "it" -> "\uD83C\uDDEE\uD83C\uDDF9"
    "ar" -> "\uD83C\uDDF8\uD83C\uDDE6"
    "tr" -> "\uD83C\uDDF9\uD83C\uDDF7"
    else -> ""
}
