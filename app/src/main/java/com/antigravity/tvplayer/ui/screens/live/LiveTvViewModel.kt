package com.antigravity.tvplayer.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.tvplayer.domain.model.Account
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.domain.repository.AccountRepository
import com.antigravity.tvplayer.domain.repository.ChannelRepository
import com.antigravity.tvplayer.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val channelRepository: ChannelRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val activeAccount: StateFlow<Account?> = accountRepository.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCategoryId = MutableStateFlow(Category.FAVORITES_ID)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val categories: StateFlow<List<Category>> = activeAccount
        .flatMapLatest { account ->
            if (account != null) {
                channelRepository.getCategories(account.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        activeAccount,
        _selectedCategoryId,
        _searchQuery
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                        val firstContent = cats.firstOrNull { it.id != Category.FAVORITES_ID }
                        if (firstContent != null && _selectedCategoryId.value == Category.FAVORITES_ID) {
                            _selectedCategoryId.value = firstContent.id
                        }
                    }
                    hasInitialized = true
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _searchQuery.value = ""
        _selectedCategoryId.value = categoryId
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
