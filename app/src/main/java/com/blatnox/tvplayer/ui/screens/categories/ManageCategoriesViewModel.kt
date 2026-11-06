package com.blatnox.tvplayer.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blatnox.tvplayer.domain.model.Account
import com.blatnox.tvplayer.domain.model.Category
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val activeAccount: StateFlow<Account?> = accountRepository.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allCategories: StateFlow<List<Category>> = activeAccount
        .flatMapLatest { account ->
            if (account != null) {
                channelRepository.getAllCategories(account.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCategories: StateFlow<List<Category>> = combine(
        allCategories,
        _searchQuery
    ) { categories, query ->
        if (query.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleCount: StateFlow<Int> = allCategories.map { cats ->
        cats.count { !it.isHidden }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategory(category: Category) {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            channelRepository.setCategoryHidden(
                accountId = account.id,
                categoryId = category.id,
                isHidden = !category.isHidden
            )
        }
    }

    fun showAll() {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            channelRepository.setAllCategoriesHidden(account.id, isHidden = false)
        }
    }

    fun hideAll() {
        val account = activeAccount.value ?: return
        viewModelScope.launch {
            channelRepository.setAllCategoriesHidden(account.id, isHidden = true)
        }
    }
}
