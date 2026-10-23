package com.antigravity.tvplayer.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.tvplayer.data.api.XtreamUrlBuilder
import com.antigravity.tvplayer.data.settings.SettingsDataStore
import com.antigravity.tvplayer.domain.model.Account
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.domain.model.StreamFormat
import com.antigravity.tvplayer.domain.repository.AccountRepository
import com.antigravity.tvplayer.domain.repository.ChannelRepository
import com.antigravity.tvplayer.domain.repository.FavoritesRepository
import com.antigravity.tvplayer.player.PlayerManager
import com.antigravity.tvplayer.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Immutable

@Immutable
data class PlayerScreenUiState(
    val currentChannel: Channel? = null,
    val isOverlayVisible: Boolean = true,
    val numberBuffer: String = "",
    val previousStreamId: Int? = null,
    val preferredFormat: StreamFormat = StreamFormat.M3U8
)

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

    private var activeAccount: Account? = null
    private var channelList: List<Channel> = emptyList()
    private var overlayHideJob: Job? = null
    private var numberBufferJob: Job? = null

    init {
        val initialStreamId: Int = savedStateHandle.get<String>("streamId")?.toIntOrNull() ?: 0

        viewModelScope.launch {
            val format = settingsDataStore.preferredStreamFormat.first()
            _uiState.update { it.copy(preferredFormat = format) }

            val account = accountRepository.getActiveAccountSync()
            activeAccount = account

            if (account != null) {
                val targetChannel = if (initialStreamId > 0) {
                    channelRepository.getChannelByStreamIdSync(account.id, initialStreamId)
                } else null

                if (targetChannel != null) {
                    playChannel(targetChannel)
                    channelRepository.getChannels(account.id, targetChannel.categoryId).collect { list ->
                        channelList = list
                    }
                } else {
                    val firstCat = channelRepository.getCategories(account.id).first()
                        .firstOrNull { it.id != Category.FAVORITES_ID }
                    val catId = firstCat?.id ?: ""
                    channelRepository.getChannels(account.id, catId).collect { list ->
                        channelList = list
                        if (_uiState.value.currentChannel == null && list.isNotEmpty()) {
                            playChannel(list.first())
                        }
                    }
                }
            }
        }
    }

    fun playChannel(channel: Channel) {
        val account = activeAccount ?: return
        val previousId = _uiState.value.currentChannel?.streamId

        _uiState.update {
            it.copy(
                currentChannel = channel,
                previousStreamId = if (previousId != null && previousId != channel.streamId) previousId else it.previousStreamId
            )
        }

        // Save last watched
        viewModelScope.launch {
            settingsDataStore.setLastWatchedStreamId(channel.streamId)
        }

        val format = _uiState.value.preferredFormat
        val primaryUrl = XtreamUrlBuilder.buildStreamUrl(
            serverUrl = account.serverUrl,
            username = account.username,
            password = account.password,
            streamId = channel.streamId,
            format = format
        )

        playerManager.play(primaryUrl) { failedUrl ->
            // Fallback between m3u8 and ts
            val fallbackFormat = if (failedUrl.endsWith(".m3u8", ignoreCase = true)) StreamFormat.TS else StreamFormat.M3U8
            XtreamUrlBuilder.buildStreamUrl(
                serverUrl = account.serverUrl,
                username = account.username,
                password = account.password,
                streamId = channel.streamId,
                format = fallbackFormat
            )
        }

        showOverlay()
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
        val target = channelList.find { it.streamId == prevId } ?: return
        playChannel(target)
    }

    fun onNumberKeyPressed(digit: Int) {
        numberBufferJob?.cancel()
        val newBuffer = (_uiState.value.numberBuffer + digit).take(4)
        _uiState.update { it.copy(numberBuffer = newBuffer) }
        showOverlay()

        numberBufferJob = viewModelScope.launch {
            delay(1500)
            val channelNum = newBuffer.toIntOrNull()
            if (channelNum != null) {
                val found = channelList.find { it.num == channelNum }
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
        overlayHideJob?.cancel()
        _uiState.update { it.copy(isOverlayVisible = true) }
        overlayHideJob = viewModelScope.launch {
            delay(4000)
            _uiState.update { it.copy(isOverlayVisible = false) }
        }
    }

    fun toggleOverlay() {
        if (_uiState.value.isOverlayVisible) {
            overlayHideJob?.cancel()
            _uiState.update { it.copy(isOverlayVisible = false) }
        } else {
            showOverlay()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        playerManager.release()
    }
}
