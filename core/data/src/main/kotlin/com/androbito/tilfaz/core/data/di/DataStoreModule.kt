package com.androbito.tilfaz.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.androbito.tilfaz.core.data.store.AccountDataStore
import com.androbito.tilfaz.core.data.store.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @AccountDataStore
    fun provideAccountDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = CoroutineScope(io + SupervisorJob())) {
        context.preferencesDataStoreFile("account")
    }

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = CoroutineScope(io + SupervisorJob())) {
        context.preferencesDataStoreFile("playback_settings")
    }
}
