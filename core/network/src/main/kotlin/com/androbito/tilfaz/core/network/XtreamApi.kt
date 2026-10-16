package com.androbito.tilfaz.core.network

import com.androbito.tilfaz.core.network.dto.AuthResponseDto
import com.androbito.tilfaz.core.network.dto.CategoryDto
import com.androbito.tilfaz.core.network.dto.LiveStreamDto
import com.androbito.tilfaz.core.network.dto.SeriesDto
import com.androbito.tilfaz.core.network.dto.SeriesInfoResponseDto
import com.androbito.tilfaz.core.network.dto.ShortEpgResponseDto
import com.androbito.tilfaz.core.network.dto.VodInfoResponseDto
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Xtream Codes player API. The server host and the username/password query parameters are added by
 * [XtreamRequestInterceptor] from the active account, so call sites never handle credentials.
 */
interface XtreamApi {

    @GET("player_api.php")
    suspend fun authenticate(): AuthResponseDto

    @GET("player_api.php?action=get_live_categories")
    suspend fun liveCategories(): List<CategoryDto>

    @GET("player_api.php?action=get_live_streams")
    suspend fun liveStreams(): List<LiveStreamDto>

    @GET("player_api.php?action=get_vod_categories")
    suspend fun vodCategories(): List<CategoryDto>

    @GET("player_api.php?action=get_vod_streams")
    suspend fun vodStreams(): List<VodStreamDto>

    @GET("player_api.php?action=get_vod_info")
    suspend fun vodInfo(@Query("vod_id") vodId: Int): VodInfoResponseDto

    @GET("player_api.php?action=get_series_categories")
    suspend fun seriesCategories(): List<CategoryDto>

    @GET("player_api.php?action=get_series")
    suspend fun series(): List<SeriesDto>

    @GET("player_api.php?action=get_series_info")
    suspend fun seriesInfo(@Query("series_id") seriesId: Int): SeriesInfoResponseDto

    @GET("player_api.php?action=get_short_epg")
    suspend fun shortEpg(@Query("stream_id") streamId: Int, @Query("limit") limit: Int = 4): ShortEpgResponseDto

    companion object {
        /** Retrofit requires a base URL at build time; the interceptor swaps it for the account's server. */
        const val PLACEHOLDER_BASE_URL = "http://xtream.placeholder.invalid/"
    }
}
