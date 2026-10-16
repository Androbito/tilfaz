package com.androbito.tilfaz.core.data.di

import com.androbito.tilfaz.core.data.repository.DataStorePlaybackSettingsRepository
import com.androbito.tilfaz.core.data.repository.DefaultAccountRepository
import com.androbito.tilfaz.core.data.repository.DefaultLiveRepository
import com.androbito.tilfaz.core.data.repository.DefaultSeriesRepository
import com.androbito.tilfaz.core.data.repository.DefaultVodRepository
import com.androbito.tilfaz.core.data.repository.RoomFavoritesRepository
import com.androbito.tilfaz.core.data.repository.RoomRecentChannelsRepository
import com.androbito.tilfaz.core.data.repository.RoomWatchProgressRepository
import com.androbito.tilfaz.core.data.store.ActiveAccountHolder
import com.androbito.tilfaz.core.data.store.KeystoreSecretCodec
import com.androbito.tilfaz.core.data.store.SecretCodec
import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.domain.repository.FavoritesRepository
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.domain.repository.RecentChannelsRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.network.ActiveAccountProvider
import com.androbito.tilfaz.core.network.UserAgentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository
    @Binds abstract fun bindLiveRepository(impl: DefaultLiveRepository): LiveRepository
    @Binds abstract fun bindVodRepository(impl: DefaultVodRepository): VodRepository
    @Binds abstract fun bindSeriesRepository(impl: DefaultSeriesRepository): SeriesRepository
    @Binds abstract fun bindFavoritesRepository(impl: RoomFavoritesRepository): FavoritesRepository
    @Binds abstract fun bindWatchProgressRepository(impl: RoomWatchProgressRepository): WatchProgressRepository
    @Binds abstract fun bindRecentChannelsRepository(impl: RoomRecentChannelsRepository): RecentChannelsRepository
    @Binds abstract fun bindPlaybackSettingsRepository(impl: DataStorePlaybackSettingsRepository): PlaybackSettingsRepository

    @Binds abstract fun bindSecretCodec(impl: KeystoreSecretCodec): SecretCodec
    @Binds abstract fun bindActiveAccountProvider(impl: ActiveAccountHolder): ActiveAccountProvider
    @Binds abstract fun bindUserAgentProvider(impl: ActiveAccountHolder): UserAgentProvider
}
