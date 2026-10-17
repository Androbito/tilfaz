package com.androbito.tilfaz.di

import com.androbito.tilfaz.BuildConfig
import com.androbito.tilfaz.feature.onboarding.SignInPrefill
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SignInPrefillModule {

    /** Empty in release builds; in debug, whatever `dev-credentials.properties` holds. */
    @Provides
    fun signInPrefill(): SignInPrefill = SignInPrefill(
        serverUrl = BuildConfig.DEV_SERVER_URL,
        username = BuildConfig.DEV_USERNAME,
        password = BuildConfig.DEV_PASSWORD
    )
}
