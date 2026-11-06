package com.blatnox.tvplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.blatnox.tvplayer.data.local.dao.AccountDao
import com.blatnox.tvplayer.data.local.dao.CategoryDao
import com.blatnox.tvplayer.data.local.dao.ChannelDao
import com.blatnox.tvplayer.data.local.dao.FavoriteDao
import com.blatnox.tvplayer.data.local.dao.RecentChannelDao
import com.blatnox.tvplayer.data.local.entity.AccountEntity
import com.blatnox.tvplayer.data.local.entity.CategoryEntity
import com.blatnox.tvplayer.data.local.entity.ChannelEntity
import com.blatnox.tvplayer.data.local.entity.FavoriteEntity
import com.blatnox.tvplayer.data.local.entity.RecentChannelEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        RecentChannelEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class TvDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentChannelDao(): RecentChannelDao
}
