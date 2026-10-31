package com.antigravity.tvplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.tvplayer.data.local.entity.ChannelEntity
import com.antigravity.tvplayer.data.local.entity.RecentChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChannelDao {

    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN recent_channels r ON c.accountId = r.accountId AND c.streamId = r.streamId
        WHERE c.accountId = :accountId
        ORDER BY r.watchedAt DESC
        LIMIT 20
    """)
    fun getRecentChannels(accountId: Long): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(entity: RecentChannelEntity)

    @Query("""
        DELETE FROM recent_channels
        WHERE accountId = :accountId AND streamId NOT IN (
            SELECT streamId FROM recent_channels
            WHERE accountId = :accountId
            ORDER BY watchedAt DESC
            LIMIT 20
        )
    """)
    suspend fun trimRecentChannels(accountId: Long)
}
