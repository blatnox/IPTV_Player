package com.antigravity.tvplayer.domain.repository

import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getCategories(accountId: Long): Flow<List<Category>>
    fun getChannels(accountId: Long, categoryId: String): Flow<List<Channel>>
    fun searchChannels(accountId: Long, query: String): Flow<List<Channel>>
    fun getChannelByStreamId(accountId: Long, streamId: Int): Flow<Channel?>
    suspend fun getChannelByStreamIdSync(accountId: Long, streamId: Int): Channel?
    suspend fun refreshData(accountId: Long, serverUrl: String, username: String, password: String): Result<Unit>
    suspend fun hasCachedData(accountId: Long): Boolean
}

interface FavoritesRepository {
    fun getFavoriteChannels(accountId: Long): Flow<List<Channel>>
    fun getFavoriteStreamIds(accountId: Long): Flow<Set<Int>>
    fun isFavorite(accountId: Long, streamId: Int): Flow<Boolean>
    suspend fun isFavoriteSync(accountId: Long, streamId: Int): Boolean
    suspend fun toggleFavorite(accountId: Long, streamId: Int)
    suspend fun addFavorite(accountId: Long, streamId: Int)
    suspend fun removeFavorite(accountId: Long, streamId: Int)
}
