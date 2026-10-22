package com.antigravity.tvplayer.di

import com.antigravity.tvplayer.data.repository.AccountRepositoryImpl
import com.antigravity.tvplayer.data.repository.ChannelRepositoryImpl
import com.antigravity.tvplayer.data.repository.FavoritesRepositoryImpl
import com.antigravity.tvplayer.domain.repository.AccountRepository
import com.antigravity.tvplayer.domain.repository.ChannelRepository
import com.antigravity.tvplayer.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(
        impl: ChannelRepositoryImpl
    ): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl
    ): FavoritesRepository
}
