package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.virtualworld.easyexpensecontrol.ui.theme.AccentPink
import kotlin.math.roundToInt

enum class CoachmarkArrowDirection {
    Up,
    Down
}

@Composable
fun TutorialCoachmarkOverlay(
    targetBounds: Rect,
    message: String,
    buttonText: String,
    arrowDirection: CoachmarkArrowDirection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrimColor = Color.Black.copy(alpha = 0.58f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val highlightPadding = with(density) { 6.dp.toPx() }
    val highlightCorner = with(density) { 14.dp.toPx() }
    val arrowHeight = with(density) { 10.dp.toPx() }
    val arrowHalfWidth = with(density) { 10.dp.toPx() }
    val balloonGap = with(density) { 12.dp.toPx() }
    val horizontalMargin = with(density) { 20.dp.toPx() }
    val balloonMaxWidth = with(density) { 280.dp.toPx() }
    val estimatedBalloonHeight = with(density) { 120.dp.toPx() }
    val interactionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            )
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val balloonWidth = balloonMaxWidth.coerceAtMost(screenWidthPx - horizontalMargin * 2)
        val balloonX = (targetBounds.center.x - balloonWidth / 2f)
            .coerceIn(horizontalMargin, screenWidthPx - balloonWidth - horizontalMargin)
        val balloonY = when (arrowDirection) {
            CoachmarkArrowDirection.Up -> targetBounds.bottom + balloonGap + arrowHeight
            CoachmarkArrowDirection.Down -> targetBounds.top - balloonGap - arrowHeight - estimatedBalloonHeight
        }.coerceAtLeast(with(density) { 16.dp.toPx() })
        val arrowCenterX = targetBounds.center.x.coerceIn(
            balloonX + arrowHalfWidth + 8f,
            balloonX + balloonWidth - arrowHalfWidth - 8f
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = scrimColor)

            val highlightRect = Rect(
                left = targetBounds.left - highlightPadding,
                top = targetBounds.top - highlightPadding,
                right = targetBounds.right + highlightPadding,
                bottom = targetBounds.bottom + highlightPadding
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = highlightRect.topLeft,
                size = highlightRect.size,
                cornerRadius = CornerRadius(highlightCorner, highlightCorner)
            )
            drawRoundRect(
                color = AccentPink,
                topLeft = highlightRect.topLeft,
                size = highlightRect.size,
                cornerRadius = CornerRadius(highlightCorner, highlightCorner),
                style = Stroke(width = with(density) { 2.5.dp.toPx() })
            )

            val arrowPath = Path()
            when (arrowDirection) {
                CoachmarkArrowDirection.Up -> {
                    arrowPath.moveTo(arrowCenterX, balloonY)
                    arrowPath.lineTo(arrowCenterX - arrowHalfWidth, balloonY + arrowHeight)
                    arrowPath.lineTo(arrowCenterX + arrowHalfWidth, balloonY + arrowHeight)
                }
                CoachmarkArrowDirection.Down -> {
                    val tipY = balloonY + estimatedBalloonHeight
                    arrowPath.moveTo(arrowCenterX, tipY)
                    arrowPath.lineTo(arrowCenterX - arrowHalfWidth, tipY - arrowHeight)
                    arrowPath.lineTo(arrowCenterX + arrowHalfWidth, tipY - arrowHeight)
                }
            }
            arrowPath.close()
            drawPath(path = arrowPath, color = surfaceColor)
        }

        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        balloonX.roundToInt(),
                        balloonY.roundToInt()
                    )
                }
                .widthIn(max = 280.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPink,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = buttonText)
                }
            }
        }
    }
}
