package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.virtualworld.easyexpensecontrol.R
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.OutlinedTextField
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TextFieldDefaults

@Composable
fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle? = null
) {
    val isDarkTheme = isSystemInDarkTheme()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            if (labelStyle != null) {
                Text(text = label, style = labelStyle)
            } else {
                Text(text = label)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .padding(5.dp)
            .fillMaxWidth(),
        enabled = !readOnly,
        readOnly = readOnly,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = if (isDarkTheme) colorResource(id = R.color.white) else colorResource(id = R.color.black),
            focusedBorderColor = if (isDarkTheme) colorResource(id = R.color.blue_green_light) else colorResource(id = R.color.blue_dark),
            unfocusedBorderColor = if (isDarkTheme) colorResource(id = R.color.blue_ultra_light) else colorResource(id = R.color.blue_transparent),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = if (isDarkTheme) colorResource(id = R.color.blue_green_light) else colorResource(id = R.color.blue_dark),
            unfocusedLabelColor = if (isDarkTheme) colorResource(id = R.color.blue_ultra_light) else colorResource(id = R.color.blue_transparent)
        )
    )
}
