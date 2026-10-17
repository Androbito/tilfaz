package com.androbito.tilfaz.core.network

import com.androbito.tilfaz.core.network.dto.AuthResponseDto
import com.androbito.tilfaz.core.network.dto.CategoryDto
import com.androbito.tilfaz.core.network.dto.SeriesInfoResponseDto
import com.androbito.tilfaz.core.network.dto.ShortEpgResponseDto
import com.androbito.tilfaz.core.network.dto.VodInfoResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Xtream Codes player API. The server host and the username/password query parameters are added by
 * [XtreamRequestInterceptor] from the active account, so call sites never handle credentials.
 */
interface XtreamApi {

    @GET("player_api.php")
    suspend fun authenticate(): AuthResponseDto

    @GET("player_api.php?action=get_live_categories")
    suspend fun liveCategories(): List<CategoryDto>

    /** The big lists are tens of megabytes of JSON: they come as a stream, read item by item with [streamArray]. */
    @Streaming
    @GET("player_api.php?action=get_live_streams")
    suspend fun liveStreams(): ResponseBody

    @GET("player_api.php?action=get_vod_categories")
    suspend fun vodCategories(): List<CategoryDto>

    @Streaming
    @GET("player_api.php?action=get_vod_streams")
    suspend fun vodStreams(): ResponseBody

    @GET("player_api.php?action=get_vod_info")
    suspend fun vodInfo(@Query("vod_id") vodId: Int): VodInfoResponseDto

    @GET("player_api.php?action=get_series_categories")
    suspend fun seriesCategories(): List<CategoryDto>

    @Streaming
    @GET("player_api.php?action=get_series")
    suspend fun series(): ResponseBody

    @GET("player_api.php?action=get_series_info")
    suspend fun seriesInfo(@Query("series_id") seriesId: Int): SeriesInfoResponseDto

    @GET("player_api.php?action=get_short_epg")
    suspend fun shortEpg(@Query("stream_id") streamId: Int, @Query("limit") limit: Int = 4): ShortEpgResponseDto

    companion object {
        /** Retrofit requires a base URL at build time; the interceptor swaps it for the account's server. */
        const val PLACEHOLDER_BASE_URL = "http://xtream.placeholder.invalid/"
    }
}
