package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.data.model.Account

const val ALL_ACCOUNTS_FILTER_ID = -1L

@Composable
fun AccountFilterDropdown(
    accounts: List<Account>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    embedded: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedAccountId) {
        ALL_ACCOUNTS_FILTER_ID -> stringResource(R.string.all_accounts)
        else -> accounts.find { it.id == selectedAccountId }?.name
            ?: stringResource(R.string.all_accounts)
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = if (embedded) {
                Color.White.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (embedded) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        if (label != null) append("$label: ")
                        append(selectedLabel)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (embedded) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_account_filter),
                    tint = if (embedded) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_accounts)) },
                onClick = {
                    onAccountSelected(ALL_ACCOUNTS_FILTER_ID)
                    expanded = false
                }
            )
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun filterTransactionsByAccount(
    transactions: List<com.virtualworld.easyexpensecontrol.data.model.Transaction>,
    accountId: Long
): List<com.virtualworld.easyexpensecontrol.data.model.Transaction> =
    if (accountId == ALL_ACCOUNTS_FILTER_ID) transactions
    else transactions.filter { it.accountId == accountId }
