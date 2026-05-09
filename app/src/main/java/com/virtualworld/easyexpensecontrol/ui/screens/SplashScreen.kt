package com.virtualworld.easyexpensecontrol.ui.screens

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.virtualworld.easyexpensecontrol.FinancialApp
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.ads.ConsentManager
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen
import kotlin.math.max
import kotlinx.coroutines.delay

private const val MAX_WAIT_MS = 10_000L
private const val POLL_INTERVAL_MS = 200L

@Composable
fun SplashScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as? FinancialApp

    var targetProgress by remember { mutableFloatStateOf(0f) }
    var navigated by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400),
        label = "splash_progress"
    )

    fun navigateToDashboard() {
        if (navigated) return
        navigated = true
        navController.navigate(Screen.DashboardScreen.route) {
            popUpTo(Screen.SplashScreen.route) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        if (activity == null || app == null || !ConsentManager.canRequestAds(context)) {
            targetProgress = 1f
            delay(600)
            navigateToDashboard()
            return@LaunchedEffect
        }

        // Tramo 0.2→0.45 mientras esperamos al manager (nunca bajar de un tick al siguiente).
        targetProgress = 0.2f
        var managerReady = false
        var elapsed = 0L

        app.runWhenAppOpenAdManagerReady { manager ->
            managerReady = true
            manager.showStartupAdIfAvailable(activity) {
                activity.runOnUiThread { navigateToDashboard() }
            }
        }

        while (!navigated && elapsed < MAX_WAIT_MS) {
            delay(POLL_INTERVAL_MS)
            elapsed += POLL_INTERVAL_MS

            val t = (elapsed.toFloat() / MAX_WAIT_MS).coerceIn(0f, 1f)
            val next = when {
                managerReady -> (0.5f + t * 0.45f).coerceAtMost(0.95f)
                else -> 0.2f + t * 0.25f // mismo tiempo: de 0.2 a 0.45
            }
            targetProgress = max(targetProgress, next)
        }

        if (!navigated) {
            targetProgress = max(targetProgress, 1f)
            delay(200)
            navigateToDashboard()
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color(0xFF165D83))
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.5f
                        scaleY = 1.5f
                    }
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                .fillMaxWidth(0.6f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = colorScheme.primary,
            trackColor = colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}
