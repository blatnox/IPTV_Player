package com.blatnox.tvplayer

import com.blatnox.tvplayer.data.local.dao.FavoriteDao
import com.blatnox.tvplayer.data.local.entity.ChannelEntity
import com.blatnox.tvplayer.data.local.entity.FavoriteEntity
import com.blatnox.tvplayer.data.repository.FavoritesRepositoryImpl
import com.blatnox.tvplayer.domain.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavoritesLogicTest {

    private lateinit var fakeFavoriteDao: FakeFavoriteDao
    private lateinit var favoritesRepository: FavoritesRepositoryImpl

    @Before
    fun setup() {
        fakeFavoriteDao = FakeFavoriteDao()
        favoritesRepository = FavoritesRepositoryImpl(fakeFavoriteDao)
    }

    @Test
    fun toggleFavorite_addsWhenNotPresent_andRemovesWhenPresent() = runBlocking {
        val accountId = 1L
        val streamId = 555

        assertFalse(favoritesRepository.isFavoriteSync(accountId, streamId))

        // Toggle 1 -> should add
        favoritesRepository.toggleFavorite(accountId, streamId)
        assertTrue(favoritesRepository.isFavoriteSync(accountId, streamId))

        // Toggle 2 -> should remove
        favoritesRepository.toggleFavorite(accountId, streamId)
        assertFalse(favoritesRepository.isFavoriteSync(accountId, streamId))
    }

    @Test
    fun favorites_areIsolatedPerAccount() = runBlocking {
        val account1 = 1L
        val account2 = 2L
        val streamId = 999

        favoritesRepository.addFavorite(account1, streamId)

        assertTrue(favoritesRepository.isFavoriteSync(account1, streamId))
        assertFalse(favoritesRepository.isFavoriteSync(account2, streamId))
    }

    @Test
    fun getFavoriteStreamIds_returnsCorrectSet() = runBlocking {
        val accountId = 1L
        favoritesRepository.addFavorite(accountId, 101)
        favoritesRepository.addFavorite(accountId, 102)

        val streamIds = favoritesRepository.getFavoriteStreamIds(accountId).first()

        assertEquals(setOf(101, 102), streamIds)
    }
}

// In-memory fake DAO for fast deterministic unit tests
class FakeFavoriteDao : FavoriteDao {
    private val favorites = mutableListOf<FavoriteEntity>()
    private val stateFlow = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    private fun notifyUpdate() {
        stateFlow.value = favorites.toList()
    }

    override fun getFavoriteStreamIds(accountId: Long): Flow<List<Int>> {
        return stateFlow.map { list ->
            list.filter { it.accountId == accountId }.map { it.streamId }
        }
    }

    override fun getFavoriteChannels(accountId: Long): Flow<List<ChannelEntity>> {
        return stateFlow.map { list ->
            list.filter { it.accountId == accountId }.map {
                ChannelEntity(
                    accountId = accountId,
                    streamId = it.streamId,
                    name = "Channel ${it.streamId}"
                )
            }
        }
    }

    override fun isFavorite(accountId: Long, streamId: Int): Flow<Boolean> {
        return stateFlow.map { list ->
            list.any { it.accountId == accountId && it.streamId == streamId }
        }
    }

    override suspend fun isFavoriteSync(accountId: Long, streamId: Int): Boolean {
        return favorites.any { it.accountId == accountId && it.streamId == streamId }
    }

    override suspend fun addFavorite(favorite: FavoriteEntity) {
        if (!favorites.any { it.accountId == favorite.accountId && it.streamId == favorite.streamId }) {
            favorites.add(favorite)
            notifyUpdate()
        }
    }

    override suspend fun removeFavorite(accountId: Long, streamId: Int) {
        favorites.removeAll { it.accountId == accountId && it.streamId == streamId }
        notifyUpdate()
    }
}
