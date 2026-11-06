package com.blatnox.tvplayer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blatnox.tvplayer.data.local.TvDatabase
import com.blatnox.tvplayer.data.local.dao.AccountDao
import com.blatnox.tvplayer.data.local.dao.CategoryDao
import com.blatnox.tvplayer.data.local.dao.ChannelDao
import com.blatnox.tvplayer.data.local.dao.FavoriteDao
import com.blatnox.tvplayer.data.local.dao.RecentChannelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_accountId_isHidden ON categories(accountId, isHidden)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_channels_accountId_categoryId")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_accountId_categoryId_num ON channels(accountId, categoryId, num)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recent_channels` (
                    `accountId` INTEGER NOT NULL,
                    `streamId` INTEGER NOT NULL,
                    `watchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`accountId`, `streamId`),
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_channels_accountId` ON `recent_channels` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_channels_accountId_watchedAt` ON `recent_channels` (`accountId`, `watchedAt`)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN backup_servers TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun provideTvDatabase(@ApplicationContext context: Context): TvDatabase {
        return Room.databaseBuilder(
            context,
            TvDatabase::class.java,
            "tv_player_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAccountDao(database: TvDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: TvDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideChannelDao(database: TvDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideFavoriteDao(database: TvDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideRecentChannelDao(database: TvDatabase): RecentChannelDao = database.recentChannelDao()
}
