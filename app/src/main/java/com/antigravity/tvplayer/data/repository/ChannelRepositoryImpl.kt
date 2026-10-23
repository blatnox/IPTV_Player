package com.antigravity.tvplayer.data.repository

import com.antigravity.tvplayer.data.api.XtreamApiService
import com.antigravity.tvplayer.data.api.XtreamUrlBuilder
import com.antigravity.tvplayer.data.local.dao.CategoryDao
import com.antigravity.tvplayer.data.local.dao.ChannelDao
import com.antigravity.tvplayer.data.local.dao.FavoriteDao
import com.antigravity.tvplayer.data.local.entity.CategoryEntity
import com.antigravity.tvplayer.data.local.entity.ChannelEntity
import com.antigravity.tvplayer.domain.model.Category
import com.antigravity.tvplayer.domain.model.Channel
import com.antigravity.tvplayer.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao
) : ChannelRepository {

    override fun getCategories(accountId: Long): Flow<List<Category>> {
        return categoryDao.getVisibleCategoriesForAccount(accountId).map { list ->
            val mapped = list.map { it.toDomain() }
            listOf(Category.favorites()) + mapped
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllCategories(accountId: Long): Flow<List<Category>> {
        return categoryDao.getAllCategoriesForAccount(accountId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun setCategoryHidden(accountId: Long, categoryId: String, isHidden: Boolean) =
        withContext(Dispatchers.IO) {
            categoryDao.setCategoryHidden(accountId, categoryId, isHidden)
        }

    override suspend fun setAllCategoriesHidden(accountId: Long, isHidden: Boolean) =
        withContext(Dispatchers.IO) {
            categoryDao.setAllCategoriesHidden(accountId, isHidden)
        }

    override fun getChannels(accountId: Long, categoryId: String): Flow<List<Channel>> {
        val channelsFlow = when {
            categoryId == Category.FAVORITES_ID -> favoriteDao.getFavoriteChannels(accountId)
            categoryId.isBlank() -> channelDao.getAllChannels(accountId)
            else -> channelDao.getChannelsByCategory(accountId, categoryId)
        }

        val favoriteIdsFlow = favoriteDao.getFavoriteStreamIds(accountId).map { it.toSet() }

        return combine(channelsFlow, favoriteIdsFlow) { channels, favIds ->
            channels.map { it.toDomain(favIds.contains(it.streamId)) }
        }.flowOn(Dispatchers.IO)
    }

    override fun searchChannels(accountId: Long, query: String): Flow<List<Channel>> {
        val searchFlow = channelDao.searchChannels(accountId, query.trim())
        val favoriteIdsFlow = favoriteDao.getFavoriteStreamIds(accountId).map { it.toSet() }

        return combine(searchFlow, favoriteIdsFlow) { channels, favIds ->
            channels.map { it.toDomain(favIds.contains(it.streamId)) }
        }.flowOn(Dispatchers.IO)
    }

    override fun getChannelByStreamId(accountId: Long, streamId: Int): Flow<Channel?> {
        val channelFlow = channelDao.getChannelByStreamId(accountId, streamId)
        val favFlow = favoriteDao.isFavorite(accountId, streamId)

        return combine(channelFlow, favFlow) { channel, isFav ->
            channel?.toDomain(isFav)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getChannelByStreamIdSync(accountId: Long, streamId: Int): Channel? =
        withContext(Dispatchers.IO) {
            val channel = channelDao.getChannelByStreamIdSync(accountId, streamId) ?: return@withContext null
            val isFav = favoriteDao.isFavoriteSync(accountId, streamId)
            channel.toDomain(isFav)
        }

    override suspend fun hasCachedData(accountId: Long): Boolean = withContext(Dispatchers.IO) {
        channelDao.getChannelCount(accountId) > 0
    }

    override suspend fun refreshData(
        accountId: Long,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiUrl = XtreamUrlBuilder.buildPlayerApiUrl(serverUrl)

            // 1. Fetch categories
            val categoriesDto = apiService.getLiveCategories(
                url = apiUrl,
                username = username,
                password = password
            )

            val categoryEntities = categoriesDto.map { dto ->
                CategoryEntity(
                    accountId = accountId,
                    categoryId = dto.categoryId,
                    categoryName = dto.categoryName.ifBlank { "Category ${dto.categoryId}" },
                    parentId = dto.parentId ?: 0
                )
            }

            // 2. Fetch streams
            val streamsDto = apiService.getLiveStreams(
                url = apiUrl,
                username = username,
                password = password
            )

            var fallbackNum = 1
            val channelEntities = streamsDto.map { dto ->
                ChannelEntity(
                    accountId = accountId,
                    streamId = dto.streamId,
                    num = dto.num ?: fallbackNum++,
                    name = dto.name.ifBlank { "Channel ${dto.streamId}" },
                    streamIcon = dto.streamIcon?.ifBlank { null },
                    epgChannelId = dto.epgChannelId?.ifBlank { null },
                    categoryId = dto.categoryId
                )
            }

            // Save to database atomically
            categoryDao.replaceCategories(accountId, categoryEntities)
            channelDao.replaceChannels(accountId, channelEntities)
        }
    }

    private fun CategoryEntity.toDomain() = Category(
        id = categoryId,
        name = categoryName,
        parentId = parentId,
        isHidden = isHidden
    )

    private fun ChannelEntity.toDomain(isFav: Boolean) = Channel(
        streamId = streamId,
        num = num,
        name = name,
        streamIcon = streamIcon,
        epgChannelId = epgChannelId,
        categoryId = categoryId,
        isFavorite = isFav
    )
}
