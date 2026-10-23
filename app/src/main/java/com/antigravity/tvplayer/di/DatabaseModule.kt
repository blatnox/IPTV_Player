package com.antigravity.tvplayer.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antigravity.tvplayer.data.local.TvDatabase
import com.antigravity.tvplayer.data.local.dao.AccountDao
import com.antigravity.tvplayer.data.local.dao.CategoryDao
import com.antigravity.tvplayer.data.local.dao.ChannelDao
import com.antigravity.tvplayer.data.local.dao.FavoriteDao
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

    @Provides
    @Singleton
    fun provideTvDatabase(@ApplicationContext context: Context): TvDatabase {
        return Room.databaseBuilder(
            context,
            TvDatabase::class.java,
            "tv_player_database"
        )
            .addMigrations(MIGRATION_1_2)
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
}
