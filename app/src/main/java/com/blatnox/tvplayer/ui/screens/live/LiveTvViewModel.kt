package com.blatnox.tvplayer.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blatnox.tvplayer.data.settings.SettingsDataStore
import com.blatnox.tvplayer.domain.model.Account
import com.blatnox.tvplayer.domain.model.Category
import com.blatnox.tvplayer.domain.model.Channel
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.domain.repository.ChannelRepository
import com.blatnox.tvplayer.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val channelRepository: ChannelRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val activeAccount: StateFlow<Account?> = accountRepository.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _selectedCategoryId = MutableStateFlow(Category.FAVORITES_ID)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastWatchedStreamId = MutableStateFlow<Int?>(null)
    val lastWatchedStreamId: StateFlow<Int?> = _lastWatchedStreamId.asStateFlow()

    var clickedStreamId: Int? = null
    var savedScrollIndex: Int = 0
    var savedScrollOffset: Int = 0
    var pendingPlayerNavigation: Boolean = false
    var shouldRestoreChannelFocus: Boolean = false

    val categories: StateFlow<List<Category>> = activeAccount
        .flatMapLatest { account ->
            if (account != null) {
                channelRepository.getCategories(account.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        activeAccount,
        _selectedCategoryId,
        _searchQuery.debounce { if (it.isBlank()) 0L else 220L }
    ) { account, categoryId, query ->
        Triple(account, categoryId, query)
    }.distinctUntilChanged()
        .flatMapLatest { (account, categoryId, query) ->
            if (account == null) {
                flowOf(emptyList())
            } else if (query.isNotBlank()) {
                channelRepository.searchChannels(account.id, query)
            } else {
                channelRepository.getChannels(account.id, categoryId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            activeAccount.collect { account ->
                if (account != null) {
                    val hasCache = channelRepository.hasCachedData(account.id)
                    if (!hasCache) {
                        refreshData()
                    }
                }
            }
        }

        viewModelScope.launch {
            var hasInitialized = false
            combine(categories, activeAccount) { cats, acc ->
                Pair(cats, acc)
            }.collect { (cats, acc) ->
                if (!hasInitialized && acc != null && cats.isNotEmpty()) {
                    val hasFavs = favoritesRepository.getFavoriteChannels(acc.id).first().isNotEmpty()
                    if (!hasFavs) {
                        val firstContent = cats.firstOrNull {
                            it.id != Category.FAVORITES_ID && it.id != Category.RECENT_ID
                        }
                        if (firstContent != null && _selectedCategoryId.value == Category.FAVORITES_ID) {
                            _selectedCategoryId.value = firstContent.id
                        }
                    }
                    hasInitialized = true
                } else if (
                    hasInitialized &&
                    cats.isNotEmpty() &&
                    _selectedCategoryId.value != Category.FAVORITES_ID &&
                    _selectedCategoryId.value != Category.RECENT_ID
                ) {
                    val exists = cats.any { it.id == _selectedCategoryId.value }
                    if (!exists) {
                        val fallback = cats.firstOrNull {
                            it.id != Category.FAVORITES_ID && it.id != Category.RECENT_ID
                        } ?: cats.first()
                        _selectedCategoryId.value = fallback.id
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsDataStore.lastWatchedStreamId.collect { streamId ->
                if ((pendingPlayerNavigation || shouldRestoreChannelFocus) && streamId != null && streamId > 0) {
                    _lastWatchedStreamId.value = streamId
                    val account = activeAccount.value
                    if (account != null && channels.value.none { it.streamId == streamId }) {
                        val watchedChannel = channelRepository.getChannelByStreamIdSync(account.id, streamId)
                        if (watchedChannel != null && watchedChannel.categoryId.isNotBlank()) {
                            _selectedCategoryId.value = watchedChannel.categoryId
                        }
                    }
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        if (_selectedCategoryId.value != categoryId) {
            clickedStreamId = null
            savedScrollIndex = 0
            savedScrollOffset = 0
            pendingPlayerNavigation = false
            shouldRestoreChannelFocus = false
            _lastWatchedStreamId.value = null
        }
        _searchQuery.value = ""
        _selectedCategoryId.value = categoryId
    }

    fun onChannelClicked(streamId: Int, firstVisibleIndex: Int, firstVisibleOffset: Int) {
        clickedStreamId = streamId
        savedScrollIndex = firstVisibleIndex
        savedScrollOffset = firstVisibleOffset
        _lastWatchedStreamId.value = streamId
        pendingPlayerNavigation = true
    }

    fun onLiveTvDisposed() {
        if (pendingPlayerNavigation) {
            pendingPlayerNavigation = false
            shouldRestoreChannelFocus = true
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(channel: Channel) {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(account.id, channel.streamId)
        }
    }

    fun refreshData() {
        val account = activeAccount.value ?: return
        if (_isRefreshing.value) return

        _isRefreshing.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = channelRepository.refreshData(
                accountId = account.id,
                serverUrl = account.serverUrl,
                username = account.username,
                password = account.password
            )
            _isRefreshing.value = false
            if (result.isFailure) {
                _errorMessage.value = "Failed to refresh channels: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
