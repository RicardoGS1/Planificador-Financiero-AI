package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue

@Composable
fun IconPickerDialog(
    selectedIconKey: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val entries = CategoryIcons.allEntries
    val currentKey = selectedIconKey ?: CategoryIcons.DEFAULT_ICON_KEY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.icon_picker_title)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.key }) { entry ->
                    val isSelected = entry.key == currentKey
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) AccentBlue
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (isSelected) Modifier.border(2.dp, AccentBlue, CircleShape)
                                else Modifier
                            )
                            .clickable { onIconSelected(entry.key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = entry.key,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
