package com.blatnox.tvplayer.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.blatnox.tvplayer.domain.model.Account
import com.blatnox.tvplayer.ui.theme.DarkBackground
import com.blatnox.tvplayer.ui.theme.ErrorRed
import com.blatnox.tvplayer.ui.theme.FocusCyan
import com.blatnox.tvplayer.ui.theme.SurfaceDark
import com.blatnox.tvplayer.ui.theme.SurfaceVariantDark
import com.blatnox.tvplayer.ui.theme.TextMuted
import com.blatnox.tvplayer.ui.theme.TextPrimary
import com.blatnox.tvplayer.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Account) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()

    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToDisplay by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(uiState.loggedInAccount) {
        val account = uiState.loggedInAccount
        if (account != null) {
            accountToDisplay = account
            showAccountDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // Left Column: Credentials Form
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = FocusCyan,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Xtream Codes IPTV Player",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "Enter your server credentials to stream live TV channels.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                TvInputField(
                    label = "Server URL (http://host:port)",
                    value = uiState.serverUrl,
                    placeholder = "http://example.com:8080",
                    onValueChange = viewModel::onServerUrlChange,
                    autoFocus = savedAccounts.isEmpty()
                )

                Spacer(modifier = Modifier.height(14.dp))

                TvInputField(
                    label = "Username",
                    value = uiState.username,
                    placeholder = "Enter username",
                    onValueChange = viewModel::onUsernameChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                TvInputField(
                    label = "Password",
                    value = uiState.password,
                    placeholder = "Enter password",
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::login,
                    onValueChange = viewModel::onPasswordChange
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B1515))
                            .border(2.dp, ErrorRed, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.titleLarge.copy(color = ErrorRed)
                        )
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = viewModel::login,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.colors(
                            containerColor = FocusCyan,
                            focusedContainerColor = Color.White
                        ),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Text(
                            text = if (uiState.isLoading) "Connecting…" else "Sign In",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = FocusCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.width(28.dp).height(28.dp)
                        )
                    }
                }
            }

            // Right Column: Saved Profiles / Accounts List
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Saved Profiles",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (savedAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved accounts yet.\nEnter credentials on the left.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedAccounts, key = { it.id }) { account ->
                            SavedAccountItem(
                                account = account,
                                isCurrent = account.id == activeAccount?.id,
                                onSelect = { viewModel.selectAccount(account) },
                                onDelete = { viewModel.deleteAccount(account.id) }
                            )
                        }
                    }
                }
            }
        }

        // Account Details Dialog after successful login
        if (showAccountDialog && accountToDisplay != null) {
            AccountInfoDialog(
                account = accountToDisplay!!,
                onDismiss = {
                    showAccountDialog = false
                    val acc = accountToDisplay!!
                    viewModel.clearLoggedInAccount()
                    onLoginSuccess(acc)
                }
            )
        }
    }
}

@Composable
fun TvInputField(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    autoFocus: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    onValueChange: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) SurfaceVariantDark else SurfaceDark)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) FocusCyan else Color(0xFF2C394F),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { focusRequester.requestFocus() },
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(color = TextMuted, fontSize = 16.sp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() }
                ),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(FocusCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
}

@Composable
fun SavedAccountItem(
    account: Account,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.colors(
            containerColor = if (isCurrent) SurfaceVariantDark else Color(0xFF1E2838),
            focusedContainerColor = FocusCyan
        ),
        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = account.serverUrl,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1
                )
                Text(
                    text = "Status: ${account.status}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (account.status.equals("Active", ignoreCase = true)) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onDelete() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete account",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
fun AccountInfoDialog(
    account: Account,
    onDismiss: () -> Unit
) {
    val expiryFormatted = remember(account.expDate) {
        val exp = account.expDate?.toLongOrNull()
        if (exp != null && exp > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp * 1000L))
        } else {
            "Unlimited / No expiry"
        }
    }

    val continueFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        continueFocusRequester.requestFocus()
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(2.dp, FocusCyan, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Authentication Successful",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = FocusCyan
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(label = "Username", value = account.username)
            InfoRow(label = "Status", value = account.status)
            InfoRow(label = "Expires", value = expiryFormatted)
            InfoRow(label = "Max Connections", value = account.maxConnections ?: "1")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(continueFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = FocusCyan,
                    focusedContainerColor = Color.White
                )
            ) {
                Text(
                    text = "Continue to Live TV",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
    }
}
