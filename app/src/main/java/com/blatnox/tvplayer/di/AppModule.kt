package com.blatnox.tvplayer.di

import com.blatnox.tvplayer.data.repository.AccountRepositoryImpl
import com.blatnox.tvplayer.data.repository.ChannelRepositoryImpl
import com.blatnox.tvplayer.data.repository.FavoritesRepositoryImpl
import com.blatnox.tvplayer.domain.repository.AccountRepository
import com.blatnox.tvplayer.domain.repository.ChannelRepository
import com.blatnox.tvplayer.domain.repository.FavoritesRepository
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
