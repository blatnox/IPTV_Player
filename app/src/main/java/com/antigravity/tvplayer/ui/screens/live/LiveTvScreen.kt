package com.antigravity.tvplayer.ui.screens.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.ui.theme.DarkBackground
import com.antigravity.tvplayer.ui.theme.FavoriteGold
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.SurfaceDark
import com.antigravity.tvplayer.ui.theme.SurfaceVariantDark
import com.antigravity.tvplayer.ui.theme.TextMuted
import com.antigravity.tvplayer.ui.theme.TextPrimary
import com.antigravity.tvplayer.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onLiveTvDisposed()
        }
    }

    LaunchedEffect(lastWatchedStreamId, channels.size) {
        val targetId = lastWatchedStreamId
        if (viewModel.shouldRestoreChannelFocus && targetId != null && channels.isNotEmpty()) {
            val targetIndex = channels.indexOfFirst { it.streamId == targetId }
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
            // Left: Categories List
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(SurfaceDark)
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                // Header & Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = FocusCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live TV",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                // Categories
                LazyColumn(
                    state = categoryListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(categoryFocusRequester)
                ) {
                    items(
                        items = categories,
                        key = { it.id },
                        contentType = { "category" }
                    ) { category ->
                        CategoryListItem(
                            category = category,
                            isSelected = category.id == selectedCategoryId,
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

            // Right: Search & Channels List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Search Input Row
                ChannelSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                                text = if (selectedCategoryId == Category.FAVORITES_ID) {
                                    "No favorites yet.\nLong-press OK on any channel to add it to Favorites."
                                } else {
                                    "No channels in this category."
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = channelListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(channelFocusRequester)
                    ) {
                        items(
                            items = channels,
                            key = { it.streamId },
                            contentType = { "channel" }
                        ) { channel ->
                            val isLastWatched = channel.streamId == lastWatchedStreamId
                            ChannelListItem(
                                channel = channel,
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
fun CategoryListItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isFocused -> FocusCyan
        isSelected -> SurfaceVariantDark
        isHovered -> SurfaceVariantDark.copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> Color.Black
        category.isFavoritesCategory -> FavoriteGold
        isSelected -> TextPrimary
        else -> TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            ),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    channel: Channel,
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
        isFocused -> FocusCyan
        isHovered -> SurfaceVariantDark
        isLastWatched -> SurfaceVariantDark.copy(alpha = 0.7f)
        else -> SurfaceDark
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(
                if (isLastWatched && !isFocused) {
                    Modifier.border(1.dp, FocusCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number
            Text(
                text = "${channel.num}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else TextMuted
                ),
                modifier = Modifier.width(44.dp)
            )

            // Channel Logo
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
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
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = if (isFocused) Color.Black else FocusCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Channel Name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) Color.Black else TextPrimary
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // Favorite Button (Directly clickable for mouse & touch)
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                        tint = if (isFocused) Color.Black else FavoriteGold,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Add favorite",
                        tint = if (isFocused) Color.Black.copy(alpha = 0.6f) else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) SurfaceVariantDark else SurfaceDark)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusCyan else Color(0xFF232D3F),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search channels by name…",
                        style = TextStyle(color = TextMuted, fontSize = 15.sp)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    interactionSource = interactionSource,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
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
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
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
        isFocused -> FocusCyan
        isHovered -> SurfaceVariantDark
        else -> SurfaceVariantDark.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
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
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color.Black else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
