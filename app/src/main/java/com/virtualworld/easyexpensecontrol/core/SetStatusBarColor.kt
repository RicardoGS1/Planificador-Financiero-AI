package com.virtualworld.easyexpensecontrol.core

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

@Composable
fun SetStatusBarColor(
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val context = LocalContext.current as ComponentActivity

    DisposableEffect(Unit) {
        context.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                statusBarColor.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                navigationBarColor.toArgb()
            )
        )

        onDispose { }
    }
}
