package com.androbito.tilfaz.core.model

import java.time.Instant

data class Series(
    val id: Int,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val rating: Rating?,
    val year: Int?,
    val lastModifiedAt: Instant?,
    val plot: String? = null,
    val genre: String? = null,
)

data class SeriesDetails(
    val series: Series,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val backdropUrls: List<String>,
    val seasons: List<Season>,
) {
    val episodeCount: Int get() = seasons.sumOf { it.episodes.size }
}

data class Season(
    val number: Int,
    val name: String,
    val episodes: List<Episode>,
)

data class Episode(
    val id: String,
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val containerExtension: String,
    val durationSeconds: Long?,
    val plot: String?,
    val thumbnailUrl: String?,
    val rating: Rating?,
) {
    /** "S01E04" style label, used in lists and as the player title. */
    val code: String get() = "S%02dE%02d".format(seasonNumber, episodeNumber)
}
