package com.blatnox.tvplayer.data.repository

import com.blatnox.tvplayer.data.local.dao.FavoriteDao
import com.blatnox.tvplayer.data.local.entity.ChannelEntity
import com.blatnox.tvplayer.data.local.entity.FavoriteEntity
import com.blatnox.tvplayer.domain.model.Channel
import com.blatnox.tvplayer.domain.model.ChannelNameFormatter
import com.blatnox.tvplayer.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun getFavoriteChannels(accountId: Long): Flow<List<Channel>> {
        return favoriteDao.getFavoriteChannels(accountId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getFavoriteStreamIds(accountId: Long): Flow<Set<Int>> {
        return favoriteDao.getFavoriteStreamIds(accountId).map { it.toSet() }
            .flowOn(Dispatchers.IO)
    }

    override fun isFavorite(accountId: Long, streamId: Int): Flow<Boolean> {
        return favoriteDao.isFavorite(accountId, streamId).flowOn(Dispatchers.IO)
    }

    override suspend fun isFavoriteSync(accountId: Long, streamId: Int): Boolean =
        withContext(Dispatchers.IO) {
            favoriteDao.isFavoriteSync(accountId, streamId)
        }

    override suspend fun toggleFavorite(accountId: Long, streamId: Int) =
        withContext(Dispatchers.IO) {
            if (favoriteDao.isFavoriteSync(accountId, streamId)) {
                favoriteDao.removeFavorite(accountId, streamId)
            } else {
                favoriteDao.addFavorite(FavoriteEntity(accountId = accountId, streamId = streamId))
            }
        }

    override suspend fun addFavorite(accountId: Long, streamId: Int) =
        withContext(Dispatchers.IO) {
            favoriteDao.addFavorite(FavoriteEntity(accountId = accountId, streamId = streamId))
        }

    override suspend fun removeFavorite(accountId: Long, streamId: Int) =
        withContext(Dispatchers.IO) {
            favoriteDao.removeFavorite(accountId, streamId)
        }

    private fun ChannelEntity.toDomain(): Channel {
        val parsed = ChannelNameFormatter.parse(name)
        return Channel(
            streamId = streamId,
            num = num,
            name = name,
            streamIcon = streamIcon,
            epgChannelId = epgChannelId,
            categoryId = categoryId,
            isFavorite = true,
            displayName = parsed.displayName,
            qualityBadge = parsed.qualityBadge,
            isSeparator = parsed.isSeparator
        )
    }
}
