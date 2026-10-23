package com.antigravity.tvplayer.ui.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.ui.theme.DarkBackground
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.SurfaceDark
import com.antigravity.tvplayer.ui.theme.SurfaceVariantDark
import com.antigravity.tvplayer.ui.theme.TextMuted
import com.antigravity.tvplayer.ui.theme.TextPrimary
import com.antigravity.tvplayer.ui.theme.TextSecondary

@Composable
fun ManageCategoriesScreen(
    viewModel: ManageCategoriesViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.filteredCategories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val visibleCount by viewModel.visibleCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    BackHandler {
        if (searchQuery.isNotEmpty()) {
            viewModel.updateSearchQuery("")
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val isBackFocused by backInteraction.collectIsFocusedAsState()
                val isBackHovered by backInteraction.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isBackFocused -> FocusCyan
                                isBackHovered -> SurfaceVariantDark
                                else -> SurfaceDark
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
                        contentDescription = "Back",
                        tint = if (isBackFocused) Color.Black else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = FocusCyan,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Manage Categories",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Select which categories appear on your Live TV list",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Counter Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, Color(0xFF232D3F), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$visibleCount / ${allCategories.size} visible",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocusCyan
                        )
                    )
                }
            }

            // Controls Row: Search + Select All + Deselect All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Input
                val searchFocus = remember { FocusRequester() }
                val searchInteraction = remember { MutableInteractionSource() }
                val isSearchFocused by searchInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSearchFocused) SurfaceVariantDark else SurfaceDark)
                        .border(
                            width = if (isSearchFocused) 2.dp else 1.dp,
                            color = if (isSearchFocused) FocusCyan else Color(0xFF232D3F),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { searchFocus.requestFocus() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isSearchFocused) FocusCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search categories by name…",
                                    style = TextStyle(color = TextMuted, fontSize = 14.sp)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                singleLine = true,
                                interactionSource = searchInteraction,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(FocusCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.updateSearchQuery("") },
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

                // Show All Button
                ActionButton(
                    text = "Show All",
                    icon = Icons.Default.Visibility,
                    onClick = viewModel::showAll
                )

                // Hide All Button
                ActionButton(
                    text = "Hide All",
                    icon = Icons.Default.VisibilityOff,
                    onClick = viewModel::hideAll
                )
            }

            // Categories List
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No categories matching \"$searchQuery\"" else "No categories available.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = categories,
                        key = { it.id },
                        contentType = { "category_toggle" }
                    ) { category ->
                        CategoryToggleItem(
                            category = category,
                            onToggle = { viewModel.toggleCategory(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryToggleItem(
    category: Category,
    onToggle: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    val isVisible = !category.isHidden

    val backgroundColor = when {
        isFocused -> FocusCyan
        isHovered -> SurfaceVariantDark
        else -> SurfaceDark
    }

    val contentColor = when {
        isFocused -> Color.Black
        isVisible -> TextPrimary
        else -> TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = when {
                        isFocused -> Color.Black
                        isVisible -> FocusCyan
                        else -> TextMuted
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isVisible || isFocused) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    ),
                    maxLines = 1
                )
            }

            // Visible / Hidden Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            isFocused -> Color.Black.copy(alpha = 0.15f)
                            isVisible -> FocusCyan.copy(alpha = 0.2f)
                            else -> Color.White.copy(alpha = 0.05f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isVisible) "Visible" else "Hidden",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFocused -> Color.Black
                            isVisible -> FocusCyan
                            else -> TextMuted
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> FocusCyan
                    isHovered -> SurfaceVariantDark
                    else -> SurfaceDark
                }
            )
            .border(
                1.dp,
                if (isFocused) FocusCyan else Color(0xFF232D3F),
                RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else TextPrimary
                )
            )
        }
    }
}
