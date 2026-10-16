package com.androbito.tilfaz.core.network.di

import com.androbito.tilfaz.core.network.BuildConfig
import com.androbito.tilfaz.core.network.XtreamApi
import com.androbito.tilfaz.core.network.XtreamRequestInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class XtreamClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** Base client without Xtream specifics; shared with the image loader and the media player. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // catalogs with 150k movies take a while on cheap panels
        .retryOnConnectionFailure(true)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        // Credentials live in the query string: never log full URLs.
                        redactQueryParams("username", "password")
                    }
                )
            }
        }
        .build()

    @Provides
    @Singleton
    @XtreamClient
    fun provideXtreamOkHttpClient(base: OkHttpClient, interceptor: XtreamRequestInterceptor): OkHttpClient =
        base.newBuilder().addInterceptor(interceptor).build()

    @Provides
    @Singleton
    fun provideXtreamApi(@XtreamClient client: OkHttpClient, json: Json): XtreamApi = Retrofit.Builder()
        .baseUrl(XtreamApi.PLACEHOLDER_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(XtreamApi::class.java)
}
