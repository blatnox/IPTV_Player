package com.antigravity.tvplayer.ui.screens.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.ui.theme.AccentBlue
import com.antigravity.tvplayer.ui.theme.Badge4K
import com.antigravity.tvplayer.ui.theme.BadgeFhd
import com.antigravity.tvplayer.ui.theme.BadgeHd
import com.antigravity.tvplayer.ui.theme.BadgeRaw
import com.antigravity.tvplayer.ui.theme.CardBorderSubtle
import com.antigravity.tvplayer.ui.theme.DarkBackground
import com.antigravity.tvplayer.ui.theme.FavoriteGold
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.SuccessGreen
import com.antigravity.tvplayer.ui.theme.SurfaceDark
import com.antigravity.tvplayer.ui.theme.SurfaceFocused
import com.antigravity.tvplayer.ui.theme.SurfaceVariantDark
import com.antigravity.tvplayer.ui.theme.TextMuted
import com.antigravity.tvplayer.ui.theme.TextPrimary
import com.antigravity.tvplayer.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel,
    onChannelSelected: (Channel) -> Unit,
    onOpenSettings: () -> Unit,
    onExitApp: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lastWatchedStreamId by viewModel.lastWatchedStreamId.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var focusInChannels by remember { mutableStateOf(viewModel.shouldRestoreChannelFocus) }

    val coroutineScope = rememberCoroutineScope()
    val categoryListState = rememberLazyListState()
    val channelListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
    )

    val categoryFocusRequester = remember { FocusRequester() }
    val channelFocusRequester = remember { FocusRequester() }
    val activeChannelFocusRequester = remember { FocusRequester() }

    // Precompute sequential channel numbers (1, 2, 3...) skipping separator rows
    val displayNumbers = remember(channels) {
        var counter = 1
        IntArray(channels.size) { idx ->
            if (channels[idx].isSeparator) 0 else counter++
        }
    }
    val playableChannelCount = remember(channels) {
        channels.count { !it.isSeparator }
    }
    val selectedCategoryName = remember(categories, selectedCategoryId, searchQuery) {
        if (searchQuery.isNotBlank()) {
            "Search Results"
        } else {
            categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Channels"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onLiveTvDisposed()
        }
    }

    LaunchedEffect(lastWatchedStreamId, channels.size) {
        val targetId = lastWatchedStreamId
        if (viewModel.shouldRestoreChannelFocus && targetId != null && channels.isNotEmpty()) {
            val targetIndex = channels.indexOfFirst { it.streamId == targetId && !it.isSeparator }
            if (targetIndex >= 0) {
                if (targetId == viewModel.clickedStreamId) {
                    channelListState.scrollToItem(viewModel.savedScrollIndex, viewModel.savedScrollOffset)
                }
                withFrameNanos { }
                val visibleIndices = channelListState.layoutInfo.visibleItemsInfo.map { it.index }
                if (targetIndex !in visibleIndices) {
                    channelListState.scrollToItem((targetIndex - 3).coerceAtLeast(0))
                    withFrameNanos { }
                }
                focusInChannels = true
                viewModel.shouldRestoreChannelFocus = false
                runCatching { activeChannelFocusRequester.requestFocus() }
            }
        }
    }

    BackHandler {
        if (searchQuery.isNotEmpty()) {
            viewModel.updateSearchQuery("")
        } else if (focusInChannels) {
            focusInChannels = false
            categoryFocusRequester.requestFocus()
        } else {
            showExitDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left: Categories Sidebar
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(SurfaceDark)
                    .padding(vertical = 18.dp, horizontal = 14.dp)
            ) {
                // App Brand & Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusCyan.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = FocusCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Live TV",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeaderIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            isLoading = isRefreshing,
                            onClick = viewModel::refreshData
                        )
                        HeaderIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings
                        )
                    }
                }

                // Categories List
                LazyColumn(
                    state = categoryListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(categoryFocusRequester)
                ) {
                    itemsIndexed(
                        items = categories,
                        key = { _, cat -> cat.id },
                        contentType = { _, _ -> "category" }
                    ) { _, category ->
                        CategoryListItem(
                            category = category,
                            isSelected = category.id == selectedCategoryId && searchQuery.isBlank(),
                            onClick = {
                                val changed = selectedCategoryId != category.id
                                viewModel.selectCategory(category.id)
                                if (changed) {
                                    coroutineScope.launch {
                                        channelListState.scrollToItem(0)
                                    }
                                }
                                focusInChannels = true
                            }
                        )
                    }
                }
            }

            // Subtle vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(CardBorderSubtle)
            )

            // Right: Top Bar + Channels List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                // Top Bar: Category Title + Channel Count + Search + Clock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    ) {
                        Text(
                            text = selectedCategoryName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$playableChannelCount ch",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocusCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ChannelSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            modifier = Modifier.width(280.dp)
                        )
                        GuideHeaderClock()
                    }
                }

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B1515))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF8A80)),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearError) {
                            Text(text = "Dismiss", color = FocusCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Channels List
                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = FocusCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading playlist channels…",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                                )
                            }
                        } else {
                            Text(
                                text = when (selectedCategoryId) {
                                    Category.FAVORITES_ID -> "No favorites yet.\nLong-press OK or click ★ on any channel to add it here."
                                    Category.RECENT_ID -> "No recently watched channels yet.\nChannels you watch will automatically appear here."
                                    else -> "No channels in this category."
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = channelListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(channelFocusRequester)
                    ) {
                        itemsIndexed(
                            items = channels,
                            key = { _, ch -> ch.streamId },
                            contentType = { _, ch -> if (ch.isSeparator) "separator" else "channel" }
                        ) { index, channel ->
                            if (channel.isSeparator) {
                                ChannelSectionDivider(title = channel.displayName)
                            } else {
                                val isLastWatched = channel.streamId == lastWatchedStreamId
                                ChannelListItem(
                                    channel = channel,
                                    displayNumber = displayNumbers[index],
                                    isLastWatched = isLastWatched,
                                    modifier = if (isLastWatched) {
                                        Modifier.focusRequester(activeChannelFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                    onClick = {
                                        focusInChannels = true
                                        viewModel.onChannelClicked(
                                            streamId = channel.streamId,
                                            firstVisibleIndex = channelListState.firstVisibleItemIndex,
                                            firstVisibleOffset = channelListState.firstVisibleItemScrollOffset
                                        )
                                        onChannelSelected(channel)
                                    },
                                    onLongClick = {
                                        viewModel.toggleFavorite(channel)
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(channel)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(text = "Exit Application", color = TextPrimary) },
                text = { Text(text = "Do you want to close the TV Player?", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                        onExitApp()
                    }) {
                        Text(text = "Exit", color = FocusCyan, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(text = "Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun GuideHeaderClock() {
    var timeText by remember { mutableStateOf("") }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = formatter.format(Date())
            delay(15_000L)
        }
    }

    Text(
        text = timeText,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    )
}

@Composable
fun ChannelSectionDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(1.dp)
                .background(FocusCyan.copy(alpha = 0.45f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = FocusCyan.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CardBorderSubtle)
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isFocused -> SurfaceFocused
        isSelected -> SurfaceVariantDark
        isHovered -> SurfaceVariantDark.copy(alpha = 0.65f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> TextPrimary
        category.isFavoritesCategory -> FavoriteGold
        category.isRecentCategory -> AccentBlue
        isSelected -> TextPrimary
        else -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(backgroundColor)
            .then(
                if (isFocused) {
                    Modifier.border(1.5.dp, FocusCyan, RoundedCornerShape(9.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left active/focus accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when {
                        isFocused -> FocusCyan
                        isSelected -> FocusCyan.copy(alpha = 0.75f)
                        else -> Color.Transparent
                    }
                )
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    channel: Channel,
    displayNumber: Int,
    isLastWatched: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isFocused -> SurfaceFocused
        isHovered -> SurfaceVariantDark
        isLastWatched -> SurfaceVariantDark.copy(alpha = 0.85f)
        else -> SurfaceDark
    }

    val borderColor = when {
        isFocused -> FocusCyan
        isLastWatched -> FocusCyan.copy(alpha = 0.45f)
        else -> CardBorderSubtle.copy(alpha = 0.65f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left focus/watched accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isFocused -> FocusCyan
                            isLastWatched -> SuccessGreen
                            else -> Color.Transparent
                        }
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Sequential Channel Number
            Text(
                text = if (displayNumber > 0) "$displayNumber" else "${channel.num}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) FocusCyan else TextMuted
                ),
                modifier = Modifier.width(38.dp)
            )

            // Channel Logo
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF0B1018))
                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.streamIcon.isNullOrBlank()) {
                    val context = LocalContext.current
                    val imageRequest = remember(channel.streamIcon) {
                        ImageRequest.Builder(context)
                            .data(channel.streamIcon)
                            .size(88, 88)
                            .allowRgb565(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = channel.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = FocusCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Channel Name
            Text(
                text = channel.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isFocused || isLastWatched) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Quality Badge Pill (4K, FHD, RAW, HD, HEVC)
            if (channel.qualityBadge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                QualityBadgePill(badge = channel.qualityBadge)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Favorite Button (Directly clickable for mouse & touch)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFavorite
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (channel.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = FavoriteGold,
                        modifier = Modifier.size(21.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Add favorite",
                        tint = if (isFocused) TextSecondary else TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QualityBadgePill(badge: String) {
    val badgeColor = when (badge) {
        "4K" -> Badge4K
        "RAW" -> BadgeRaw
        "FHD" -> BadgeFhd
        else -> BadgeHd
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun ChannelSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isFocused) SurfaceFocused else SurfaceDark)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) FocusCyan else CardBorderSubtle,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) FocusCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search channels…",
                        style = TextStyle(color = TextMuted, fontSize = 14.sp)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FocusCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isFocused -> SurfaceFocused
        isHovered -> SurfaceVariantDark
        else -> SurfaceVariantDark.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isFocused) Modifier.border(1.5.dp, FocusCyan, RoundedCornerShape(8.dp))
                else Modifier.border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = FocusCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) FocusCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
