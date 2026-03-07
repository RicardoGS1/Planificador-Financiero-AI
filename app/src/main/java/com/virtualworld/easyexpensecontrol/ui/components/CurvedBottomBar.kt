package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.virtualworld.easyexpensecontrol.ui.theme.AccentPink
import com.virtualworld.easyexpensecontrol.ui.navigation.Screen

private val BottomBarHeight = 72.dp
private val FabSize = 56.dp
private val NotchRadius = 32.dp

data class CurvedNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun CurvedBottomBar(navController: NavController) {
    val items = listOf(
        CurvedNavItem(Screen.DashboardScreen.route, "Inicio", Icons.Outlined.Home),
        CurvedNavItem(Screen.HistoryScreen.route, "Historial", Icons.Outlined.History),
        CurvedNavItem(Screen.StaticsScreen.route, "Estadísticas", Icons.Outlined.BarChart),
        CurvedNavItem(Screen.BudgetScreen.route, "Presupuestos", Icons.Outlined.AccountBalanceWallet)
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val barColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight + 24.dp)
    ) {
        // Barra con curva (recorte central para el FAB)
        CurvedBarShape(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .align(Alignment.BottomCenter)
                .background(barColor)
                .clip(CurvedBottomShape(NotchRadius)),
            notchRadius = NotchRadius
        )

        // Contenido: 2 ítems a la izquierda, hueco central, 2 ítems a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Izquierda: Inicio y Historial
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.take(2).forEach { item ->
                    CurvedBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }

            // Centro: espacio para el FAB (no dibujamos nada, el FAB va encima)
            Box(modifier = Modifier.size(FabSize + 16.dp))

            // Derecha: Estadísticas y Presupuestos
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.drop(2).forEach { item ->
                    CurvedBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }

        // FAB central (añadir entrada)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-BottomBarHeight / 2).minus(8.dp))
                .size(FabSize)
                .background(AccentPink, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate(Screen.AddEditTransactionScreen.route + "/0L")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir entrada",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun CurvedBarItem(
    item: CurvedNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AccentPink else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = iconColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** Dibuja la máscara/recorte opcional si hace falta; el layout ya usa CurvedBottomShape para clip. */
@Composable
private fun CurvedBarShape(
    modifier: Modifier,
    notchRadius: Dp
) {
    // Solo ocupamos espacio; el clip del modifier ya da la forma
    Box(modifier = modifier)
}

/** Forma con curva cóncava central (muesca para el FAB). */
private class CurvedBottomShape(private val notchRadius: Dp) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val r = with(density) { notchRadius.toPx() }
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, 0f)
            lineTo(cx - r, 0f)
            // Arco inferior del semicírculo (curva cóncava hacia abajo)
            arcTo(
                rect = Rect(cx - r, 0f, cx + r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w, 0f)
            lineTo(w, h)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
