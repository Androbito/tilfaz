package com.androbito.tilfaz.core.network.dto

import com.androbito.tilfaz.core.network.serialization.EpisodesBySeasonSerializer
import com.androbito.tilfaz.core.network.serialization.LenientBooleanSerializer
import com.androbito.tilfaz.core.network.serialization.LenientDoubleSerializer
import com.androbito.tilfaz.core.network.serialization.LenientIntSerializer
import com.androbito.tilfaz.core.network.serialization.LenientLongSerializer
import com.androbito.tilfaz.core.network.serialization.LenientStringListSerializer
import com.androbito.tilfaz.core.network.serialization.LenientStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CategoryDto(
    @SerialName("category_id") @Serializable(LenientStringSerializer::class) val categoryId: String? = null,
    @SerialName("category_name") @Serializable(LenientStringSerializer::class) val categoryName: String? = null,
    @SerialName("parent_id") @Serializable(LenientStringSerializer::class) val parentId: String? = null,
)

@Serializable
data class LiveStreamDto(
    @Serializable(LenientIntSerializer::class) val num: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @SerialName("stream_type") @Serializable(LenientStringSerializer::class) val streamType: String? = null,
    @SerialName("stream_id") @Serializable(LenientIntSerializer::class) val streamId: Int? = null,
    @SerialName("stream_icon") @Serializable(LenientStringSerializer::class) val streamIcon: String? = null,
    @SerialName("epg_channel_id") @Serializable(LenientStringSerializer::class) val epgChannelId: String? = null,
    @Serializable(LenientLongSerializer::class) val added: Long? = null,
    @SerialName("category_id") @Serializable(LenientStringSerializer::class) val categoryId: String? = null,
    @SerialName("tv_archive") @Serializable(LenientBooleanSerializer::class) val tvArchive: Boolean? = null,
    @SerialName("tv_archive_duration") @Serializable(LenientIntSerializer::class) val tvArchiveDuration: Int? = null,
)

@Serializable
data class VodStreamDto(
    @Serializable(LenientIntSerializer::class) val num: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @SerialName("stream_id") @Serializable(LenientIntSerializer::class) val streamId: Int? = null,
    @SerialName("stream_icon") @Serializable(LenientStringSerializer::class) val streamIcon: String? = null,
    @Serializable(LenientDoubleSerializer::class) val rating: Double? = null,
    @SerialName("rating_5based") @Serializable(LenientDoubleSerializer::class) val rating5Based: Double? = null,
    @Serializable(LenientLongSerializer::class) val added: Long? = null,
    @SerialName("category_id") @Serializable(LenientStringSerializer::class) val categoryId: String? = null,
    @SerialName("container_extension") @Serializable(LenientStringSerializer::class) val containerExtension: String? = null,
    @Serializable(LenientIntSerializer::class) val year: Int? = null,
    @SerialName("release_date") @Serializable(LenientStringSerializer::class) val releaseDate: String? = null,
)

@Serializable
data class VodInfoResponseDto(
    val info: VodInfoDto? = null,
    @SerialName("movie_data") val movieData: VodMovieDataDto? = null,
)

@Serializable
data class VodInfoDto(
    @SerialName("movie_image") @Serializable(LenientStringSerializer::class) val movieImage: String? = null,
    @Serializable(LenientStringSerializer::class) val plot: String? = null,
    @Serializable(LenientStringSerializer::class) val cast: String? = null,
    @Serializable(LenientStringSerializer::class) val director: String? = null,
    @Serializable(LenientStringSerializer::class) val genre: String? = null,
    @SerialName("releasedate") @Serializable(LenientStringSerializer::class) val releaseDate: String? = null,
    @SerialName("release_date") @Serializable(LenientStringSerializer::class) val releaseDateAlt: String? = null,
    @SerialName("duration_secs") @Serializable(LenientLongSerializer::class) val durationSeconds: Long? = null,
    @Serializable(LenientStringSerializer::class) val duration: String? = null,
    @Serializable(LenientDoubleSerializer::class) val rating: Double? = null,
    @SerialName("backdrop_path") @Serializable(LenientStringListSerializer::class) val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") @Serializable(LenientStringSerializer::class) val youtubeTrailer: String? = null,
)

@Serializable
data class VodMovieDataDto(
    @SerialName("stream_id") @Serializable(LenientIntSerializer::class) val streamId: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @Serializable(LenientLongSerializer::class) val added: Long? = null,
    @SerialName("category_id") @Serializable(LenientStringSerializer::class) val categoryId: String? = null,
    @SerialName("container_extension") @Serializable(LenientStringSerializer::class) val containerExtension: String? = null,
)

@Serializable
data class SeriesDto(
    @Serializable(LenientIntSerializer::class) val num: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @SerialName("series_id") @Serializable(LenientIntSerializer::class) val seriesId: Int? = null,
    @Serializable(LenientStringSerializer::class) val cover: String? = null,
    @Serializable(LenientStringSerializer::class) val plot: String? = null,
    @Serializable(LenientStringSerializer::class) val cast: String? = null,
    @Serializable(LenientStringSerializer::class) val director: String? = null,
    @Serializable(LenientStringSerializer::class) val genre: String? = null,
    @SerialName("releaseDate") @Serializable(LenientStringSerializer::class) val releaseDate: String? = null,
    @SerialName("release_date") @Serializable(LenientStringSerializer::class) val releaseDateAlt: String? = null,
    @SerialName("last_modified") @Serializable(LenientLongSerializer::class) val lastModified: Long? = null,
    @Serializable(LenientDoubleSerializer::class) val rating: Double? = null,
    @SerialName("rating_5based") @Serializable(LenientDoubleSerializer::class) val rating5Based: Double? = null,
    @SerialName("backdrop_path") @Serializable(LenientStringListSerializer::class) val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") @Serializable(LenientStringSerializer::class) val youtubeTrailer: String? = null,
    @SerialName("category_id") @Serializable(LenientStringSerializer::class) val categoryId: String? = null,
)

@Serializable
data class SeriesInfoResponseDto(
    val seasons: List<SeasonDto> = emptyList(),
    val info: SeriesDto? = null,
    @Serializable(EpisodesBySeasonSerializer::class) val episodes: Map<String, List<JsonElement>> = emptyMap(),
)

@Serializable
data class SeasonDto(
    @SerialName("season_number") @Serializable(LenientIntSerializer::class) val seasonNumber: Int? = null,
    @Serializable(LenientStringSerializer::class) val name: String? = null,
    @SerialName("episode_count") @Serializable(LenientIntSerializer::class) val episodeCount: Int? = null,
    @Serializable(LenientStringSerializer::class) val cover: String? = null,
    @SerialName("air_date") @Serializable(LenientStringSerializer::class) val airDate: String? = null,
)

@Serializable
data class EpisodeDto(
    @Serializable(LenientStringSerializer::class) val id: String? = null,
    @SerialName("episode_num") @Serializable(LenientIntSerializer::class) val episodeNum: Int? = null,
    @Serializable(LenientStringSerializer::class) val title: String? = null,
    @SerialName("container_extension") @Serializable(LenientStringSerializer::class) val containerExtension: String? = null,
    @Serializable(LenientIntSerializer::class) val season: Int? = null,
    @Serializable(LenientLongSerializer::class) val added: Long? = null,
    val info: EpisodeInfoDto? = null,
)

@Serializable
data class EpisodeInfoDto(
    @SerialName("movie_image") @Serializable(LenientStringSerializer::class) val movieImage: String? = null,
    @Serializable(LenientStringSerializer::class) val plot: String? = null,
    @SerialName("duration_secs") @Serializable(LenientLongSerializer::class) val durationSeconds: Long? = null,
    @Serializable(LenientStringSerializer::class) val duration: String? = null,
    @Serializable(LenientDoubleSerializer::class) val rating: Double? = null,
    @SerialName("releasedate") @Serializable(LenientStringSerializer::class) val releaseDate: String? = null,
)

@Serializable
data class ShortEpgResponseDto(
    @SerialName("epg_listings") val listings: List<EpgListingDto> = emptyList(),
)

@Serializable
data class EpgListingDto(
    @Serializable(LenientStringSerializer::class) val id: String? = null,
    /** Base64 encoded by Xtream panels. */
    @Serializable(LenientStringSerializer::class) val title: String? = null,
    @Serializable(LenientStringSerializer::class) val description: String? = null,
    @Serializable(LenientStringSerializer::class) val lang: String? = null,
    @Serializable(LenientStringSerializer::class) val start: String? = null,
    @Serializable(LenientStringSerializer::class) val end: String? = null,
    @SerialName("start_timestamp") @Serializable(LenientLongSerializer::class) val startTimestamp: Long? = null,
    @SerialName("stop_timestamp") @Serializable(LenientLongSerializer::class) val stopTimestamp: Long? = null,
    @SerialName("now_playing") @Serializable(LenientBooleanSerializer::class) val nowPlaying: Boolean? = null,
    @SerialName("has_archive") @Serializable(LenientBooleanSerializer::class) val hasArchive: Boolean? = null,
)
