package com.blatnox.tvplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.blatnox.tvplayer.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE accountId = :accountId ORDER BY num ASC, name ASC")
    fun getAllChannels(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND categoryId = :categoryId ORDER BY num ASC, name ASC")
    fun getChannelsByCategory(accountId: Long, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND name LIKE '%' || :query || '%' ORDER BY num ASC, name ASC LIMIT 300")
    fun searchChannels(accountId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND streamId = :streamId LIMIT 1")
    fun getChannelByStreamId(accountId: Long, streamId: Int): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND streamId = :streamId LIMIT 1")
    suspend fun getChannelByStreamIdSync(accountId: Long, streamId: Int): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels WHERE accountId = :accountId")
    suspend fun getChannelCount(accountId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE accountId = :accountId")
    suspend fun deleteChannelsForAccount(accountId: Long)

    @Transaction
    suspend fun replaceChannels(accountId: Long, channels: List<ChannelEntity>) {
        deleteChannelsForAccount(accountId)
        channels.chunked(5000).forEach { chunk ->
            insertChannels(chunk)
        }
    }
}
