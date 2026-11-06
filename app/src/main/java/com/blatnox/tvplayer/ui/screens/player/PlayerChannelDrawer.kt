package com.blatnox.tvplayer.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.blatnox.tvplayer.domain.model.Category
import com.blatnox.tvplayer.domain.model.Channel
import com.blatnox.tvplayer.ui.screens.live.QualityBadgePill
import com.blatnox.tvplayer.ui.theme.AccentBlue
import com.blatnox.tvplayer.ui.theme.CardBorderSubtle
import com.blatnox.tvplayer.ui.theme.FavoriteGold
import com.blatnox.tvplayer.ui.theme.FocusCyan
import com.blatnox.tvplayer.ui.theme.SuccessGreen
import com.blatnox.tvplayer.ui.theme.SurfaceDark
import com.blatnox.tvplayer.ui.theme.SurfaceFocused
import com.blatnox.tvplayer.ui.theme.SurfaceVariantDark
import com.blatnox.tvplayer.ui.theme.TextMuted
import com.blatnox.tvplayer.ui.theme.TextPrimary
import com.blatnox.tvplayer.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PlayerChannelDrawer(
    visible: Boolean,
    categories: List<Category>,
    selectedCategoryId: String,
    channels: List<Channel>,
    currentStreamId: Int?,
    onSelectCategory: (String) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = visible) {
        onClose()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val categoryListState = rememberLazyListState()
        val channelListState = rememberLazyListState()
        val activeChannelFocusRequester = remember { FocusRequester() }
        val firstChannelFocusRequester = remember { FocusRequester() }
        val selectedCategoryFocusRequester = remember { FocusRequester() }

        val displayNumbers = remember(channels) {
            var counter = 1
            IntArray(channels.size) { idx ->
                if (channels[idx].isSeparator) 0 else counter++
            }
        }
        val firstPlayableIndex = remember(channels) {
            channels.indexOfFirst { !it.isSeparator }
        }

        LaunchedEffect(visible) {
            if (visible) {
                val catIdx = categories.indexOfFirst { it.id == selectedCategoryId }
                if (catIdx > 0) {
                    categoryListState.scrollToItem((catIdx - 3).coerceAtLeast(0))
                }
                val chIdx = channels.indexOfFirst { it.streamId == currentStreamId && !it.isSeparator }
                if (chIdx >= 0) {
                    channelListState.scrollToItem((chIdx - 3).coerceAtLeast(0))
                    withFrameNanos { }
                    runCatching { activeChannelFocusRequester.requestFocus() }
                } else if (firstPlayableIndex >= 0) {
                    channelListState.scrollToItem(0)
                    withFrameNanos { }
                    runCatching { firstChannelFocusRequester.requestFocus() }
                } else {
                    withFrameNanos { }
                    runCatching { selectedCategoryFocusRequester.requestFocus() }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Drawer Panel
            Column(
                modifier = Modifier
                    .width(590.dp)
                    .fillMaxHeight()
                    .background(Color(0xEE0B1018))
            ) {
                // Drawer Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = FocusCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Channel Guide",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "• Live Playing",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    val closeInteraction = remember { MutableInteractionSource() }
                    val isCloseFocused by closeInteraction.collectIsFocusedAsState()
                    val isCloseHovered by closeInteraction.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCloseFocused -> SurfaceFocused
                                    isCloseHovered -> SurfaceVariantDark
                                    else -> SurfaceVariantDark.copy(alpha = 0.6f)
                                }
                            )
                            .then(
                                if (isCloseFocused) Modifier.border(1.5.dp, FocusCyan, CircleShape)
                                else Modifier
                            )
                            .clickable(
                                interactionSource = closeInteraction,
                                indication = null,
                                onClick = onClose
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Guide",
                            tint = if (isCloseFocused) FocusCyan else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorderSubtle)
                )

                // Split: Categories (Left) + Channels (Right)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Categories Column
                    LazyColumn(
                        state = categoryListState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxHeight()
                            .background(SurfaceDark.copy(alpha = 0.55f))
                    ) {
                        itemsIndexed(
                            items = categories,
                            key = { _, cat -> cat.id },
                            contentType = { _, _ -> "drawer_cat" }
                        ) { _, category ->
                            val isSelected = category.id == selectedCategoryId
                            DrawerCategoryItem(
                                category = category,
                                isSelected = isSelected,
                                modifier = if (isSelected) {
                                    Modifier.focusRequester(selectedCategoryFocusRequester)
                                } else {
                                    Modifier
                                },
                                onClick = {
                                    if (selectedCategoryId != category.id) {
                                        onSelectCategory(category.id)
                                        coroutineScope.launch {
                                            channelListState.scrollToItem(0)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(CardBorderSubtle)
                    )

                    // Channels Column
                    if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedCategoryId) {
                                    Category.FAVORITES_ID -> "No favorite channels yet."
                                    Category.RECENT_ID -> "No recent channels yet."
                                    else -> "No channels in category."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = channelListState,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            contentPadding = PaddingValues(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(
                                items = channels,
                                key = { _, ch -> ch.streamId },
                                contentType = { _, ch -> if (ch.isSeparator) "sep" else "ch" }
                            ) { index, channel ->
                                if (channel.isSeparator) {
                                    Text(
                                        text = channel.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FocusCyan.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                } else {
                                    val isPlayingNow = channel.streamId == currentStreamId
                                    val itemModifier = when {
                                        isPlayingNow -> Modifier.focusRequester(activeChannelFocusRequester)
                                        index == firstPlayableIndex -> Modifier.focusRequester(firstChannelFocusRequester)
                                        else -> Modifier
                                    }
                                    DrawerChannelItem(
                                        channel = channel,
                                        displayNumber = displayNumbers[index],
                                        isPlayingNow = isPlayingNow,
                                        modifier = itemModifier,
                                        onClick = { onSelectChannel(channel) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right border of drawer
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(FocusCyan.copy(alpha = 0.25f))
            )

            // Click outside area (closes drawer on mouse click)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { onClose() }
                    }
            )
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    category: Category,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    val bg = when {
        isFocused -> SurfaceFocused
        isSelected -> SurfaceVariantDark
        isHovered -> SurfaceVariantDark.copy(alpha = 0.65f)
        else -> Color.Transparent
    }
    val textColor = when {
        isFocused -> TextPrimary
        category.isFavoritesCategory -> FavoriteGold
        category.isRecentCategory -> AccentBlue
        isSelected -> TextPrimary
        else -> TextSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (isFocused) Modifier.border(1.5.dp, FocusCyan, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when {
                        isFocused -> FocusCyan
                        isSelected -> FocusCyan.copy(alpha = 0.7f)
                        else -> Color.Transparent
                    }
                )
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerChannelItem(
    channel: Channel,
    displayNumber: Int,
    isPlayingNow: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    val bg = when {
        isFocused -> SurfaceFocused
        isHovered -> SurfaceVariantDark
        isPlayingNow -> SurfaceVariantDark.copy(alpha = 0.85f)
        else -> SurfaceDark.copy(alpha = 0.8f)
    }
    val border = when {
        isFocused -> FocusCyan
        isPlayingNow -> SuccessGreen.copy(alpha = 0.6f)
        else -> CardBorderSubtle.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (displayNumber > 0) "$displayNumber" else "${channel.num}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isFocused) FocusCyan else TextMuted
            ),
            modifier = Modifier.width(28.dp)
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.streamIcon.isNullOrBlank()) {
                val context = LocalContext.current
                val req = remember(channel.streamIcon) {
                    ImageRequest.Builder(context)
                        .data(channel.streamIcon)
                        .size(64, 64)
                        .allowRgb565(true)
                        .build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = channel.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = FocusCyan.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = channel.displayName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isFocused || isPlayingNow) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isPlayingNow) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
        } else if (channel.qualityBadge != null) {
            Spacer(modifier = Modifier.width(6.dp))
            QualityBadgePill(badge = channel.qualityBadge)
        }
    }
}
