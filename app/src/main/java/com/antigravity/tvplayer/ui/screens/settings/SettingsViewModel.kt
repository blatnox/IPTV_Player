package com.antigravity.tvplayer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.tvplayer.data.api.XtreamUrlBuilder
import com.antigravity.tvplayer.data.settings.SettingsDataStore
import com.antigravity.tvplayer.domain.model.Account
import com.antigravity.tvplayer.domain.model.StreamFormat
import com.antigravity.tvplayer.domain.repository.AccountRepository
import com.antigravity.tvplayer.domain.repository.ChannelRepository
import com.antigravity.tvplayer.domain.repository.ServerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val channelRepository: ChannelRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val activeAccount: StateFlow<Account?> = accountRepository.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAccounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferredStreamFormat: StateFlow<StreamFormat> = settingsDataStore.preferredStreamFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreamFormat.M3U8)

    val resumeLastChannel: StateFlow<Boolean> = settingsDataStore.resumeLastChannel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customUserAgent: StateFlow<String> = settingsDataStore.customUserAgent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_USER_AGENT)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshStatus = MutableStateFlow<String?>(null)
    val refreshStatus: StateFlow<String?> = _refreshStatus.asStateFlow()

    private val _serverStatuses = MutableStateFlow<Map<String, ServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = _serverStatuses.asStateFlow()

    private val _isTestingServers = MutableStateFlow(false)
    val isTestingServers: StateFlow<Boolean> = _isTestingServers.asStateFlow()

    fun updatePrimaryServer(newServerUrl: String) {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            accountRepository.setPrimaryServer(account.id, newServerUrl)
        }
    }

    fun addBackupServer(url: String) {
        val account = activeAccount.value ?: return
        val current = account.backupServers
        val normalized = XtreamUrlBuilder.normalizeServerUrl(url)
        if (normalized.isNotBlank() && !current.contains(normalized) && normalized != account.serverUrl) {
            viewModelScope.launch {
                accountRepository.updateBackupServers(account.id, current + normalized)
            }
        }
    }

    fun removeBackupServer(url: String) {
        val account = activeAccount.value ?: return
        val current = account.backupServers
        viewModelScope.launch {
            accountRepository.updateBackupServers(account.id, current.filter { it != url })
        }
    }

    fun promoteBackupToPrimary(backupUrl: String) {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            accountRepository.setPrimaryServer(account.id, backupUrl)
        }
    }

    fun testAllServers() {
        val account = activeAccount.value ?: return
        val allUrls = (listOf(account.serverUrl) + account.backupServers).distinct()
        if (_isTestingServers.value || allUrls.isEmpty()) return

        _isTestingServers.value = true
        viewModelScope.launch {
            val results = mutableMapOf<String, ServerStatus>()
            for (url in allUrls) {
                val status = accountRepository.testServerLatency(url)
                results[url] = status
            }
            _serverStatuses.value = results
            _isTestingServers.value = false
        }
    }

    fun setStreamFormat(format: StreamFormat) {
        viewModelScope.launch {
            settingsDataStore.setPreferredStreamFormat(format)
        }
    }

    fun setResumeLastChannel(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setResumeLastChannel(enabled)
        }
    }

    fun setCustomUserAgent(userAgent: String) {
        viewModelScope.launch {
            settingsDataStore.setCustomUserAgent(userAgent)
        }
    }

    fun switchAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.switchAccount(accountId)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun refreshPlaylist() {
        val account = activeAccount.value ?: return
        if (_isRefreshing.value) return

        _isRefreshing.value = true
        _refreshStatus.value = "Refreshing categories and channels…"

        viewModelScope.launch {
            val result = channelRepository.refreshData(
                accountId = account.id,
                serverUrl = account.serverUrl,
                username = account.username,
                password = account.password
            )
            _isRefreshing.value = false
            if (result.isSuccess) {
                _refreshStatus.value = "Playlist refreshed successfully!"
            } else {
                _refreshStatus.value = "Refresh failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun logoutCurrentAccount(onLoggedOut: () -> Unit) {
        val account = activeAccount.value
        viewModelScope.launch {
            if (account != null) {
                accountRepository.deleteAccount(account.id)
            }
            onLoggedOut()
        }
    }
}
