package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import android.app.Activity
import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.ads.AiRewardedAdHelper
import com.virtualworld.easyexpensecontrol.ui.theme.AccentBlue

/**
 * Diálogo informativo previo al rewarded ad de acceso a funciones de IA (políticas AdMob).
 * Si el anuncio aún no está precargado, muestra estado de carga y lo presenta al estar listo.
 */
@Composable
fun AiAccessRewardedDialog(
    context: Context,
    onDismiss: () -> Unit,
    onShowAd: (Activity) -> Unit,
    onLoadFailed: () -> Unit
) {
    var isLoadingAd by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AiRewardedAdHelper.preload(context)
    }

    fun dismiss() {
        AiRewardedAdHelper.cancelPendingLoadWait()
        isLoadingAd = false
        onDismiss()
    }

    fun tryShowAd() {
        val activity = context as? Activity ?: run {
            loadFailed = true
            isLoadingAd = false
            onLoadFailed()
            return
        }
        if (AiRewardedAdHelper.isAdReady()) {
            isLoadingAd = false
            loadFailed = false
            onShowAd(activity)
            return
        }
        isLoadingAd = true
        loadFailed = false
        AiRewardedAdHelper.whenAdReady(
            context = context,
            onReady = {
                isLoadingAd = false
                loadFailed = false
                onShowAd(activity)
            },
            onFailed = {
                isLoadingAd = false
                loadFailed = true
                onLoadFailed()
            }
        )
    }
    val infinite = rememberInfiniteTransition(label = "ai_access_dialog")
    val iconScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Dialog(
        onDismissRequest = {
            if (!isLoadingAd) dismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLoadingAd,
            dismissOnClickOutside = !isLoadingAd
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AiAccessDialogHeader(iconScale = iconScale)

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_access_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isLoadingAd) {
                            stringResource(R.string.ai_access_loading_ad)
                        } else if (loadFailed) {
                            stringResource(R.string.ai_access_ad_failed)
                        } else {
                            stringResource(R.string.ai_access_dialog_message)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (loadFailed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isLoadingAd) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AiAccessFeatureChip(
                            icon = Icons.Filled.CameraAlt,
                            label = stringResource(R.string.ai_access_feature_scan),
                            modifier = Modifier.weight(1f)
                        )
                        AiAccessFeatureChip(
                            icon = Icons.Filled.Mic,
                            label = stringResource(R.string.ai_access_feature_voice),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AiAccessFeatureChip(
                        icon = Icons.Filled.TableChart,
                        label = stringResource(R.string.ai_access_feature_excel),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_access_session_hint),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { tryShowAd() },
                        enabled = !isLoadingAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoadingAd) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ai_access_loading_ad),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (loadFailed) R.string.ai_access_retry_ad
                                    else R.string.ai_access_watch_ad
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { dismiss() },
                        enabled = !isLoadingAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_access_not_now),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAccessDialogHeader(iconScale: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AccentBlue,
                        Color(0xFF6BB1FF),
                        Color(0xFF9C6BFF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(iconScale),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AiAccessFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
