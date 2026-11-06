package com.blatnox.tvplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blatnox.tvplayer.data.local.entity.ChannelEntity
import com.blatnox.tvplayer.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT streamId FROM favorites WHERE accountId = :accountId ORDER BY addedAt DESC")
    fun getFavoriteStreamIds(accountId: Long): Flow<List<Int>>

    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN favorites f ON c.accountId = f.accountId AND c.streamId = f.streamId
        WHERE c.accountId = :accountId
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteChannels(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE accountId = :accountId AND streamId = :streamId)")
    fun isFavorite(accountId: Long, streamId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE accountId = :accountId AND streamId = :streamId)")
    suspend fun isFavoriteSync(accountId: Long, streamId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE accountId = :accountId AND streamId = :streamId")
    suspend fun removeFavorite(accountId: Long, streamId: Int)
}
