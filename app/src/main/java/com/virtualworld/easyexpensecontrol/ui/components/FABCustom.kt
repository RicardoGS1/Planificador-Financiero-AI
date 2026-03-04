package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen

@Composable
fun FABCustom(navController: NavController) {
    val isDarkTheme = isSystemInDarkTheme()

    if (isDarkTheme) {
        FloatingActionButton(
            modifier = Modifier.padding(all = 20.dp),
            containerColor = Color.White,
            onClick = {
                navController.navigate(Screen.AddEditTransactionScreen.route + "/0L")
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir transacciones",
                tint = Color.Black
            )
        }
    } else {
        FloatingActionButton(
            modifier = Modifier.padding(all = 20.dp),
            containerColor = Color.Black,
            onClick = {
                navController.navigate(Screen.AddEditTransactionScreen.route + "/0L")
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir transacciones",
                tint = Color.White
            )
        }
    }
}
