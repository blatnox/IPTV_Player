package com.antigravity.tvplayer.ui.screens.settings

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.antigravity.tvplayer.domain.model.Account
import com.antigravity.tvplayer.domain.model.StreamFormat
import com.antigravity.tvplayer.ui.screens.login.InfoRow
import com.antigravity.tvplayer.ui.theme.DarkBackground
import com.antigravity.tvplayer.ui.theme.ErrorRed
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.SurfaceDark
import com.antigravity.tvplayer.ui.theme.SurfaceVariantDark
import com.antigravity.tvplayer.ui.theme.TextMuted
import com.antigravity.tvplayer.ui.theme.TextPrimary
import com.antigravity.tvplayer.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAddNewAccount: () -> Unit
) {
    val activeAccount by viewModel.activeAccount.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val streamFormat by viewModel.preferredStreamFormat.collectAsState()
    val resumeLastChannel by viewModel.resumeLastChannel.collectAsState()
    val userAgent by viewModel.customUserAgent.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    BackHandler {
        onBack()
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
                modifier = Modifier.padding(bottom = 20.dp)
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
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = FocusCyan,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Player & Account Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // Two columns layout for settings
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Playback & Stream Settings
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SettingsSectionTitle(title = "Stream & Playback")
                    }

                    item {
                        SettingsCard(title = "Preferred Stream Format") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StreamFormatButton(
                                    label = "HLS (.m3u8)",
                                    isSelected = streamFormat == StreamFormat.M3U8,
                                    onClick = { viewModel.setStreamFormat(StreamFormat.M3U8) },
                                    modifier = Modifier.weight(1f)
                                )
                                StreamFormatButton(
                                    label = "MPEG-TS (.ts)",
                                    isSelected = streamFormat == StreamFormat.TS,
                                    onClick = { viewModel.setStreamFormat(StreamFormat.TS) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "If the stream fails, player will automatically retry with the fallback format.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    item {
                        SettingsCard(title = "Startup Behavior") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Resume Last Watched Channel",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Automatically plays the last channel upon opening the app.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.setResumeLastChannel(!resumeLastChannel) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (resumeLastChannel) FocusCyan else SurfaceVariantDark,
                                        focusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.clickable { viewModel.setResumeLastChannel(!resumeLastChannel) }
                                ) {
                                    Text(
                                        text = if (resumeLastChannel) "Enabled" else "Disabled",
                                        color = if (resumeLastChannel) Color.Black else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SettingsCard(title = "Network & Playlist") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reload Channels & Categories",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Syncs latest streams from the Xtream server. Favorites are preserved.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                Button(
                                    onClick = viewModel::refreshPlaylist,
                                    enabled = !isRefreshing,
                                    colors = ButtonDefaults.colors(
                                        containerColor = FocusCyan,
                                        focusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.clickable { if (!isRefreshing) viewModel.refreshPlaylist() }
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            color = Color.Black,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Refresh",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (refreshStatus != null) {
                                Text(
                                    text = refreshStatus ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocusCyan),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    item {
                        SettingsCard(title = "HTTP User-Agent") {
                            Text(
                                text = userAgent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Standard User-Agent ensures maximum compatibility with Xtream proxy streams.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Right Column: Active Account & Profile Switcher
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SettingsSectionTitle(title = "Active Account")
                    }

                    if (activeAccount != null) {
                        item {
                            ActiveAccountCard(account = activeAccount!!)
                        }
                    }

                    item {
                        SettingsSectionTitle(title = "Profiles / Accounts")
                    }

                    items(allAccounts.size) { index ->
                        val acc = allAccounts[index]
                        AccountManagementRow(
                            account = acc,
                            isActive = acc.id == activeAccount?.id,
                            onSelect = { viewModel.switchAccount(acc.id) },
                            onDelete = { viewModel.deleteAccount(acc.id) }
                        )
                    }

                    item {
                        Button(
                            onClick = onAddNewAccount,
                            colors = ButtonDefaults.colors(
                                containerColor = SurfaceVariantDark,
                                focusedContainerColor = FocusCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add New Xtream Account",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = FocusCyan
        ),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun StreamFormatButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) FocusCyan else SurfaceVariantDark,
            focusedContainerColor = Color.White
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.Black else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActiveAccountCard(account: Account) {
    val expiryFormatted = remember(account.expDate) {
        val exp = account.expDate?.toLongOrNull()
        if (exp != null && exp > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp * 1000L))
        } else {
            "Unlimited"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, FocusCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow(label = "Username", value = account.username)
            InfoRow(label = "Server", value = account.serverUrl)
            InfoRow(label = "Status", value = account.status)
            InfoRow(label = "Expiry Date", value = expiryFormatted)
            InfoRow(label = "Max Connections", value = account.maxConnections ?: "1")
        }
    }
}

@Composable
fun AccountManagementRow(
    account: Account,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.colors(
            containerColor = if (isActive) SurfaceVariantDark else SurfaceDark,
            focusedContainerColor = FocusCyan
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username + if (isActive) " (Active)" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = account.serverUrl,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1
                )
            }

            Surface(
                onClick = onDelete,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = ErrorRed
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
