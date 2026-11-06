package com.blatnox.tvplayer.ui.screens.player

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blatnox.tvplayer.data.api.XtreamUrlBuilder
import com.blatnox.tvplayer.data.settings.SettingsDataStore
import com.blatnox.tvplayer.domain.model.Account
import com.blatnox.tvplayer.domain.model.Category
import com.blatnox.tvplayer.domain.model.Channel
import com.blatnox.tvplayer.domain.model.StreamFormat
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.domain.repository.ChannelRepository
import com.blatnox.tvplayer.domain.repository.FavoritesRepository
import com.blatnox.tvplayer.player.PlayerManager
import com.blatnox.tvplayer.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PlayerScreenUiState(
    val currentChannel: Channel? = null,
    val currentChannelDisplayNumber: Int = 0,
    val isOverlayVisible: Boolean = true,
    val isChannelDrawerOpen: Boolean = false,
    val numberBuffer: String = "",
    val previousStreamId: Int? = null,
    val preferredFormat: StreamFormat = StreamFormat.M3U8
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val channelRepository: ChannelRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsDataStore: SettingsDataStore,
    val playerManager: PlayerManager
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = playerManager.uiState

    private val _uiState = MutableStateFlow(PlayerScreenUiState())
    val uiState: StateFlow<PlayerScreenUiState> = _uiState.asStateFlow()

    private val _activeAccountFlow = MutableStateFlow<Account?>(null)
    private val _activeZapCategoryId = MutableStateFlow("")
    private val _drawerSelectedCategoryId = MutableStateFlow("")
    val drawerSelectedCategoryId: StateFlow<String> = _drawerSelectedCategoryId.asStateFlow()

    val drawerCategories: StateFlow<List<Category>> = _activeAccountFlow
        .flatMapLatest { acc ->
            if (acc != null) channelRepository.getCategories(acc.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val drawerChannels: StateFlow<List<Channel>> = combine(
        _activeAccountFlow,
        _drawerSelectedCategoryId
    ) { acc, catId ->
        Pair(acc, catId)
    }.flatMapLatest { (acc, catId) ->
        if (acc == null || catId.isBlank()) {
            flowOf(emptyList())
        } else {
            channelRepository.getChannels(acc.id, catId)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var activeAccount: Account? = null
    private var sessionActiveServerUrl: String? = null
    private var channelList: List<Channel> = emptyList()
    private var overlayHideJob: Job? = null
    private var numberBufferJob: Job? = null

    init {
        playerManager.onStreamSuccess = { successfulUrl ->
            activeAccount?.let { acc ->
                val allServers = (listOf(acc.serverUrl) + acc.backupServers).distinct()
                val matched = allServers.firstOrNull { s ->
                    val host = try { java.net.URI(s).host } catch (_: Exception) { null }
                    host != null && successfulUrl.contains(host, ignoreCase = true)
                }
                if (matched != null && matched != sessionActiveServerUrl) {
                    sessionActiveServerUrl = matched
                }
            }
        }

        val initialStreamId: Int = savedStateHandle.get<String>("streamId")?.toIntOrNull() ?: 0

        viewModelScope.launch {
            val format = settingsDataStore.preferredStreamFormat.first()
            _uiState.update { it.copy(preferredFormat = format) }

            val account = accountRepository.getActiveAccountSync()
            activeAccount = account
            _activeAccountFlow.value = account

            if (account != null) {
                val targetChannel = if (initialStreamId > 0) {
                    channelRepository.getChannelByStreamIdSync(account.id, initialStreamId)
                } else null

                if (targetChannel != null) {
                    _activeZapCategoryId.value = targetChannel.categoryId
                    _drawerSelectedCategoryId.value = targetChannel.categoryId
                    playChannel(targetChannel)
                } else {
                    val firstCat = channelRepository.getCategories(account.id).first()
                        .firstOrNull { it.id != Category.FAVORITES_ID && it.id != Category.RECENT_ID }
                    val catId = firstCat?.id ?: ""
                    _activeZapCategoryId.value = catId
                    _drawerSelectedCategoryId.value = catId
                }

                _activeZapCategoryId.flatMapLatest { catId ->
                    if (catId.isBlank()) flowOf(emptyList())
                    else channelRepository.getChannels(account.id, catId)
                }.collect { rawList ->
                    val playable = rawList.filter { !it.isSeparator }
                    channelList = playable
                    val current = _uiState.value.currentChannel
                    if (current == null && playable.isNotEmpty()) {
                        playChannel(playable.first())
                    } else if (current != null) {
                        val idx = playable.indexOfFirst { it.streamId == current.streamId }
                        if (idx >= 0) {
                            _uiState.update { it.copy(currentChannelDisplayNumber = idx + 1) }
                        }
                    }
                }
            }
        }
    }

    fun playChannel(channel: Channel) {
        if (channel.isSeparator) return
        val account = activeAccount ?: return
        val previousId = _uiState.value.currentChannel?.streamId
        val seqIdx = channelList.indexOfFirst { it.streamId == channel.streamId }

        _uiState.update {
            it.copy(
                currentChannel = channel,
                currentChannelDisplayNumber = if (seqIdx >= 0) seqIdx + 1 else 0,
                previousStreamId = if (previousId != null && previousId != channel.streamId) previousId else it.previousStreamId
            )
        }

        // Save last watched & add to Recent Channels history (capped at 20 rows)
        viewModelScope.launch {
            settingsDataStore.setLastWatchedStreamId(channel.streamId)
            channelRepository.recordRecentChannel(account.id, channel.streamId)
        }

        val format = _uiState.value.preferredFormat
        val fallbackFormat = if (format == StreamFormat.M3U8) StreamFormat.TS else StreamFormat.M3U8

        // Order servers: previously working session server first, then primary, then configured backup mirrors
        val orderedServers = mutableListOf<String>()
        val workingServer = sessionActiveServerUrl
        if (!workingServer.isNullOrBlank()) {
            orderedServers.add(workingServer)
        }
        orderedServers.add(account.serverUrl)
        orderedServers.addAll(account.backupServers)

        val uniqueServers = orderedServers
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val candidates = mutableListOf<String>()
        for (server in uniqueServers) {
            candidates.add(
                XtreamUrlBuilder.buildStreamUrl(
                    serverUrl = server,
                    username = account.username,
                    password = account.password,
                    streamId = channel.streamId,
                    format = format
                )
            )
            candidates.add(
                XtreamUrlBuilder.buildStreamUrl(
                    serverUrl = server,
                    username = account.username,
                    password = account.password,
                    streamId = channel.streamId,
                    format = fallbackFormat
                )
            )
        }

        playerManager.play(candidates)
        showOverlay()
    }

    fun openChannelDrawer() {
        overlayHideJob?.cancel()
        val currentCat = _uiState.value.currentChannel?.categoryId
        if (_drawerSelectedCategoryId.value.isBlank() && !currentCat.isNullOrBlank()) {
            _drawerSelectedCategoryId.value = currentCat
        }
        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                isChannelDrawerOpen = true
            )
        }
    }

    fun closeChannelDrawer() {
        _uiState.update { it.copy(isChannelDrawerOpen = false) }
    }

    fun selectDrawerCategory(categoryId: String) {
        _drawerSelectedCategoryId.value = categoryId
    }

    fun playChannelFromDrawer(channel: Channel) {
        if (channel.isSeparator) return
        val selectedCat = _drawerSelectedCategoryId.value
        if (selectedCat.isNotBlank()) {
            _activeZapCategoryId.value = selectedCat
        }
        _uiState.update { it.copy(isChannelDrawerOpen = false) }
        playChannel(channel)
    }

    fun zapNext() {
        if (channelList.isEmpty()) return
        val current = _uiState.value.currentChannel ?: return
        val currentIndex = channelList.indexOfFirst { it.streamId == current.streamId }
        val nextIndex = if (currentIndex >= 0 && currentIndex < channelList.size - 1) {
            currentIndex + 1
        } else {
            0
        }
        playChannel(channelList[nextIndex])
    }

    fun zapPrevious() {
        if (channelList.isEmpty()) return
        val current = _uiState.value.currentChannel ?: return
        val currentIndex = channelList.indexOfFirst { it.streamId == current.streamId }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            channelList.size - 1
        }
        playChannel(channelList[prevIndex])
    }

    fun zapLastChannel() {
        val prevId = _uiState.value.previousStreamId ?: return
        val inCurrentList = channelList.find { it.streamId == prevId }
        if (inCurrentList != null) {
            playChannel(inCurrentList)
        } else {
            val account = activeAccount ?: return
            viewModelScope.launch {
                val target = channelRepository.getChannelByStreamIdSync(account.id, prevId)
                if (target != null) {
                    _activeZapCategoryId.value = target.categoryId
                    _drawerSelectedCategoryId.value = target.categoryId
                    playChannel(target)
                }
            }
        }
    }

    fun onNumberKeyPressed(digit: Int) {
        numberBufferJob?.cancel()
        val newBuffer = (_uiState.value.numberBuffer + digit).take(4)
        _uiState.update { it.copy(numberBuffer = newBuffer) }
        showOverlay()

        numberBufferJob = viewModelScope.launch {
            delay(1400)
            val channelNum = newBuffer.toIntOrNull()
            if (channelNum != null && channelNum > 0) {
                val found = if (channelNum <= channelList.size) {
                    channelList[channelNum - 1]
                } else {
                    channelList.find { it.num == channelNum }
                }
                if (found != null) {
                    playChannel(found)
                }
            }
            _uiState.update { it.copy(numberBuffer = "") }
        }
    }

    fun toggleCurrentFavorite() {
        val channel = _uiState.value.currentChannel ?: return
        val account = activeAccount ?: return

        viewModelScope.launch {
            favoritesRepository.toggleFavorite(account.id, channel.streamId)
            val isFav = favoritesRepository.isFavoriteSync(account.id, channel.streamId)
            _uiState.update {
                it.copy(currentChannel = channel.copy(isFavorite = isFav))
            }
        }
        showOverlay()
    }

    fun showOverlay() {
        if (_uiState.value.isChannelDrawerOpen) return
        overlayHideJob?.cancel()
        _uiState.update { it.copy(isOverlayVisible = true) }
        overlayHideJob = viewModelScope.launch {
            delay(4000)
            _uiState.update { it.copy(isOverlayVisible = false) }
        }
    }

    fun toggleOverlay() {
        if (_uiState.value.isChannelDrawerOpen) {
            closeChannelDrawer()
            return
        }
        if (_uiState.value.isOverlayVisible) {
            overlayHideJob?.cancel()
            _uiState.update { it.copy(isOverlayVisible = false) }
        } else {
            showOverlay()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.onStreamSuccess = null
        playerManager.stop()
        playerManager.release()
    }
}
