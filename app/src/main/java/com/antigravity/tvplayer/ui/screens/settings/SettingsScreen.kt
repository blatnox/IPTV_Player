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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.antigravity.tvplayer.domain.repository.ServerStatus
import com.antigravity.tvplayer.ui.screens.login.InfoRow
import com.antigravity.tvplayer.ui.theme.CardBorderSubtle
import com.antigravity.tvplayer.ui.theme.DarkBackground
import com.antigravity.tvplayer.ui.theme.ErrorRed
import com.antigravity.tvplayer.ui.theme.FocusCyan
import com.antigravity.tvplayer.ui.theme.SuccessGreen
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
    onAddNewAccount: () -> Unit,
    onManageCategories: () -> Unit
) {
    val activeAccount by viewModel.activeAccount.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val streamFormat by viewModel.preferredStreamFormat.collectAsState()
    val resumeLastChannel by viewModel.resumeLastChannel.collectAsState()
    val userAgent by viewModel.customUserAgent.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()
    val serverStatuses by viewModel.serverStatuses.collectAsState()
    val isTestingServers by viewModel.isTestingServers.collectAsState()
    var showAddMirrorDialog by remember { mutableStateOf(false) }

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
                        SettingsCard(title = "Categories") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Manage Categories",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Select or hide categories to simplify your Live TV guide.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }

                                val manageInteraction = remember { MutableInteractionSource() }
                                val isManageFocused by manageInteraction.collectIsFocusedAsState()
                                val isManageHovered by manageInteraction.collectIsHoveredAsState()

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isManageFocused -> FocusCyan
                                                isManageHovered -> SurfaceVariantDark
                                                else -> SurfaceVariantDark.copy(alpha = 0.7f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isManageFocused) FocusCyan else Color(0xFF232D3F),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(
                                            interactionSource = manageInteraction,
                                            indication = null,
                                            onClick = onManageCategories
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (isManageFocused) Color.Black else FocusCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Manage",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isManageFocused) Color.Black else TextPrimary
                                            )
                                        )
                                    }
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
                        item {
                            ServerMirrorsCard(
                                account = activeAccount!!,
                                serverStatuses = serverStatuses,
                                isTesting = isTestingServers,
                                onTestServers = viewModel::testAllServers,
                                onPromote = viewModel::promoteBackupToPrimary,
                                onRemove = viewModel::removeBackupServer,
                                onAddClick = { showAddMirrorDialog = true }
                            )
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

        if (showAddMirrorDialog) {
            AddMirrorDialog(
                onDismiss = { showAddMirrorDialog = false },
                onConfirm = { url ->
                    viewModel.addBackupServer(url)
                    showAddMirrorDialog = false
                }
            )
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

@Composable
fun ServerMirrorsCard(
    account: Account,
    serverStatuses: Map<String, ServerStatus>,
    isTesting: Boolean,
    onTestServers: () -> Unit,
    onPromote: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddClick: () -> Unit
) {
    SettingsCard(title = "Server Mirrors & Failover") {
        Text(
            text = "If playback drops (e.g. 502/timeout), the player automatically retries across configured backup mirrors.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Primary Server Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariantDark)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FocusCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRIMARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = account.serverUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }

                val primaryStatus = serverStatuses[account.serverUrl]
                if (primaryStatus != null) {
                    ServerLatencyPill(status = primaryStatus)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Backup Mirrors
        Text(
            text = "Backup Mirrors (${account.backupServers.size})",
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (account.backupServers.isEmpty()) {
            Text(
                text = "No backup mirrors added. Add backup URLs provided by your service for seamless stream recovery.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                account.backupServers.forEach { mirrorUrl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161E2C))
                            .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mirrorUrl,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                                maxLines = 1
                            )
                            val status = serverStatuses[mirrorUrl]
                            if (status != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                ServerLatencyPill(status = status)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPromote(mirrorUrl) },
                                colors = ButtonDefaults.colors(
                                    containerColor = SurfaceVariantDark,
                                    focusedContainerColor = FocusCyan
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "Make Primary",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Surface(
                                onClick = { onRemove(mirrorUrl) },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.Transparent,
                                    focusedContainerColor = ErrorRed
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete mirror",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Action Buttons: Test & Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTestServers,
                enabled = !isTesting,
                colors = ButtonDefaults.colors(
                    containerColor = SurfaceVariantDark,
                    focusedContainerColor = FocusCyan
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = FocusCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Testing…", style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = FocusCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test All Mirrors",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.colors(
                    containerColor = FocusCyan,
                    focusedContainerColor = Color.White
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Mirror",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ServerLatencyPill(status: ServerStatus) {
    val (bg, textColor) = if (status.isOnline) {
        Pair(SuccessGreen.copy(alpha = 0.18f), SuccessGreen)
    } else {
        Pair(ErrorRed.copy(alpha = 0.18f), ErrorRed)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (status.isOnline) "${status.latencyMs} ms" else "Offline",
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun AddMirrorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textVal by remember { mutableStateOf("http://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Backup Mirror URL",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter an alternative server provided by your IPTV service (e.g. http://pro.uhd8.top):",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, FocusCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = textVal,
                        onValueChange = { textVal = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (textVal.isNotBlank() && textVal != "http://") {
                                    onConfirm(textVal.trim())
                                }
                            }
                        ),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(FocusCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textVal.isNotBlank() && textVal != "http://") {
                        onConfirm(textVal.trim())
                    }
                }
            ) {
                Text(text = "Add Mirror", color = FocusCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    )
}
