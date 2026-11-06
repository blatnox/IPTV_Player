package com.blatnox.tvplayer.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blatnox.tvplayer.domain.model.Channel
import com.blatnox.tvplayer.player.PlayerUiState
import com.blatnox.tvplayer.ui.theme.CardBorderSubtle
import com.blatnox.tvplayer.ui.theme.ErrorRed
import com.blatnox.tvplayer.ui.theme.FavoriteGold
import com.blatnox.tvplayer.ui.theme.FocusCyan
import com.blatnox.tvplayer.ui.theme.SuccessGreen
import com.blatnox.tvplayer.ui.theme.SurfaceDark
import com.blatnox.tvplayer.ui.theme.SurfaceFocused
import com.blatnox.tvplayer.ui.theme.TextMuted
import com.blatnox.tvplayer.ui.theme.TextPrimary
import com.blatnox.tvplayer.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerOverlay(
    channel: Channel?,
    displayNumber: Int = 0,
    playerState: PlayerUiState,
    isVisible: Boolean,
    numberBuffer: String,
    onBack: () -> Unit,
    onOpenChannelDrawer: () -> Unit,
    onZapPrevious: () -> Unit,
    onZapNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetry: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-Left Floating Back & Quick Guide Buttons
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 32.dp, vertical = 26.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerControlButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Channel List",
                    onClick = onBack
                )

                val guideInteraction = remember { MutableInteractionSource() }
                val isGuideFocused by guideInteraction.collectIsFocusedAsState()
                val isGuideHovered by guideInteraction.collectIsHoveredAsState()
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            when {
                                isGuideFocused -> SurfaceFocused
                                isGuideHovered -> Color(0xEE182334)
                                else -> Color(0xD80C121D)
                            }
                        )
                        .border(
                            width = if (isGuideFocused) 1.5.dp else 1.dp,
                            color = if (isGuideFocused) FocusCyan else CardBorderSubtle,
                            shape = RoundedCornerShape(21.dp)
                        )
                        .clickable(
                            interactionSource = guideInteraction,
                            indication = null,
                            onClick = onOpenChannelDrawer
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Open Channel Guide",
                        tint = FocusCyan,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Channels (▶)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE00B1018))
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 28.dp, vertical = 22.dp)
                ) {
                    CircularProgressIndicator(
                        color = FocusCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            playerState.isUsingBackupServer -> "Connecting to backup mirror (${playerState.activeServerHost.ifEmpty { "mirror" }})…"
                            playerState.retryAttempt > 0 -> "Reconnecting (${playerState.retryAttempt}/3)…"
                            playerState.isUsingFallback -> "Connecting via fallback…"
                            else -> "Buffering…"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xF00F1724))
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
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
                        text = "Press OK to retry, ▶ for Channel Guide, or ▲ / ▼ to switch channels.",
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
                    .padding(horizontal = 36.dp, vertical = 28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEB0B1018))
                    .border(1.5.dp, FocusCyan, RoundedCornerShape(12.dp))
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "CH $numberBuffer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocusCyan
                    )
                )
            }
        }

        // Floating Bottom Info Card
        AnimatedVisibility(
            visible = isVisible && channel != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp, vertical = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEB0C1320))
                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Logo + Channel Title + Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF080C14))
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel?.streamIcon.isNullOrBlank()) {
                                val context = LocalContext.current
                                val imageReq = remember(channel!!.streamIcon) {
                                    ImageRequest.Builder(context)
                                        .data(channel.streamIcon)
                                        .size(116, 116)
                                        .allowRgb565(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageReq,
                                    contentDescription = channel.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(6.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = FocusCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val chNumber = if (displayNumber > 0) displayNumber else channel?.num ?: 0
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(FocusCyan.copy(alpha = 0.16f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CH $chNumber",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = FocusCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = channel?.displayName ?: "",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 21.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // LIVE Green Pill
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuccessGreen.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    )
                                }

                                StreamBadge(
                                    text = if (playerState.currentStreamUrl.endsWith(".m3u8", ignoreCase = true)) "HLS" else "MPEG-TS"
                                )
                                if (playerState.videoHeight > 0) {
                                    val resLabel = when {
                                        playerState.videoHeight >= 2160 -> "4K UHD"
                                        playerState.videoHeight >= 1080 -> "1080p FHD"
                                        playerState.videoHeight >= 720 -> "720p HD"
                                        else -> "${playerState.videoHeight}p SD"
                                    }
                                    StreamBadge(text = resLabel)
                                }
                                if (playerState.isUsingBackupServer) {
                                    val mirrorText = if (playerState.activeServerHost.isNotEmpty()) "Mirror: ${playerState.activeServerHost}" else "Mirror"
                                    StreamBadge(text = mirrorText, color = FocusCyan)
                                } else if (playerState.isUsingFallback) {
                                    StreamBadge(text = "Fallback", color = Color(0xFFFF9800))
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▲/▼ Zap  •  ▶ Guide  •  ◀ Prev Ch",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Right: Playback Controls + Favorite + Clock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlayerControlButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Channel Guide",
                            onClick = onOpenChannelDrawer
                        )

                        PlayerControlButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Channel",
                            onClick = onZapPrevious
                        )

                        PlayerControlButton(
                            icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            onClick = onTogglePlayPause
                        )

                        PlayerControlButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next Channel",
                            onClick = onZapNext
                        )

                        val favInteraction = remember { MutableInteractionSource() }
                        val isFavFocused by favInteraction.collectIsFocusedAsState()
                        val isFavHovered by favInteraction.collectIsHoveredAsState()

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isFavFocused -> SurfaceFocused
                                        isFavHovered -> Color.White.copy(alpha = 0.2f)
                                        else -> SurfaceDark
                                    }
                                )
                                .border(
                                    width = if (isFavFocused) 1.5.dp else 1.dp,
                                    color = if (isFavFocused) FocusCyan else CardBorderSubtle,
                                    shape = CircleShape
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
                                tint = if (channel?.isFavorite == true) FavoriteGold else TextPrimary,
                                modifier = Modifier.size(21.dp)
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
        style = MaterialTheme.typography.titleLarge.copy(
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
            .size(42.dp)
            .clip(CircleShape)
            .background(
                when {
                    isFocused -> SurfaceFocused
                    isHovered -> Color.White.copy(alpha = 0.2f)
                    else -> Color(0xD80C121D)
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) FocusCyan else CardBorderSubtle,
                shape = CircleShape
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
            tint = if (isFocused) FocusCyan else TextPrimary,
            modifier = Modifier.size(20.dp)
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
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
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
