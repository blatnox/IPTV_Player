package com.antigravity.tvplayer.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.player.PlayerUiState
import com.antigravity.tvplayer.ui.theme.ErrorRed
import com.antigravity.tvplayer.ui.theme.FavoriteGold
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.OverlayBackground
import com.antigravity.tvplayer.ui.theme.SurfaceDark
import com.antigravity.tvplayer.ui.theme.TextMuted
import com.antigravity.tvplayer.ui.theme.TextPrimary
import com.antigravity.tvplayer.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerOverlay(
    channel: Channel?,
    playerState: PlayerUiState,
    isVisible: Boolean,
    numberBuffer: String,
    onBack: () -> Unit,
    onZapPrevious: () -> Unit,
    onZapNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-Left Back Button for Mouse & Remote
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            val isBackFocused by backInteraction.collectIsFocusedAsState()
            val isBackHovered by backInteraction.collectIsHoveredAsState()

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isBackFocused -> FocusCyan
                            isBackHovered -> Color.White.copy(alpha = 0.35f)
                            else -> Color.Black.copy(alpha = 0.65f)
                        }
                    )
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Channel List",
                    tint = if (isBackFocused) Color.Black else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        // Center Buffering Indicator
        if (playerState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = FocusCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            playerState.retryAttempt > 0 -> "Reconnecting (${playerState.retryAttempt}/3)…"
                            playerState.isUsingFallback -> "Connecting via fallback…"
                            else -> "Buffering…"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                }
            }
        }

        // Center Error Notification
        if (playerState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark.copy(alpha = 0.92f))
                        .padding(horizontal = 32.dp, vertical = 22.dp)
                ) {
                    Text(
                        text = "Stream Unavailable",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val retryInteraction = remember { MutableInteractionSource() }
                    val isRetryHovered by retryInteraction.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRetryHovered) FocusCyan.copy(alpha = 0.85f) else FocusCyan)
                            .clickable(
                                interactionSource = retryInteraction,
                                indication = null,
                                onClick = onRetry
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Retry Stream (OK)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Press OK to retry or Up / Down to switch channels.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }

        // Top-Right Numeric Zap Overlay (when user presses 0-9 on remote)
        if (numberBuffer.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Channel: $numberBuffer",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusCyan
                    )
                )
            }
        }

        // Bottom Info Overlay with Gradient
        AnimatedVisibility(
            visible = isVisible && channel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Logo + Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Channel Logo
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel?.streamIcon.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel!!.streamIcon,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(6.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = FocusCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${channel?.num}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = FocusCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = channel?.name ?: "",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StreamBadge(
                                    text = if (playerState.currentStreamUrl.endsWith(".m3u8", ignoreCase = true)) "HLS" else "TS"
                                )
                                if (playerState.videoHeight > 0) {
                                    val resLabel = when {
                                        playerState.videoHeight >= 2160 -> "4K"
                                        playerState.videoHeight >= 1080 -> "1080p FHD"
                                        playerState.videoHeight >= 720 -> "720p HD"
                                        else -> "SD"
                                    }
                                    StreamBadge(text = resLabel)
                                }
                                if (playerState.isUsingFallback) {
                                    StreamBadge(text = "Fallback TS", color = Color(0xFFFF9800))
                                }
                            }
                        }
                    }

                    // Right: Playback Controls + Favorite + Clock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Channel Zap Previous Button
                        PlayerControlButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Channel",
                            onClick = onZapPrevious
                        )

                        // Play / Pause Toggle Button
                        PlayerControlButton(
                            icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            onClick = onTogglePlayPause
                        )

                        // Channel Zap Next Button
                        PlayerControlButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Channel",
                            onClick = onZapNext
                        )

                        // Favorite Toggle Button
                        val favInteraction = remember { MutableInteractionSource() }
                        val isFavFocused by favInteraction.collectIsFocusedAsState()
                        val isFavHovered by favInteraction.collectIsHoveredAsState()

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isFavFocused -> FocusCyan
                                        isFavHovered -> Color.White.copy(alpha = 0.35f)
                                        else -> Color.White.copy(alpha = 0.15f)
                                    }
                                )
                                .clickable(
                                    interactionSource = favInteraction,
                                    indication = null,
                                    onClick = onToggleFavorite
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (channel?.isFavorite == true) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavFocused) Color.Black else if (channel?.isFavorite == true) FavoriteGold else TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OverlayClock()
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayClock() {
    var currentTime by remember { mutableStateOf("") }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormatter.format(Date())
            delay(10_000L)
        }
    }

    Text(
        text = currentTime,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    )
}

@Composable
fun PlayerControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    isFocused -> FocusCyan
                    isHovered -> Color.White.copy(alpha = 0.35f)
                    else -> Color.White.copy(alpha = 0.15f)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.Black else TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun StreamBadge(
    text: String,
    color: Color = FocusCyan
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}
