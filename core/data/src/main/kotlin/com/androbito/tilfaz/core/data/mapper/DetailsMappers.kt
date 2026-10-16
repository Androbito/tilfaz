package com.androbito.tilfaz.core.data.mapper

import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.Rating
import com.androbito.tilfaz.core.model.Season
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.network.dto.EpisodeDto
import com.androbito.tilfaz.core.network.dto.SeriesInfoResponseDto
import com.androbito.tilfaz.core.network.dto.VodInfoResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

fun VodInfoResponseDto.toDomain(fallback: Movie): MovieDetails {
    val info = info
    val data = movieData
    val movie = fallback.copy(
        name = data?.name?.trim()?.ifEmpty { null } ?: fallback.name,
        posterUrl = info?.movieImage?.takeIf { it.startsWith("http") } ?: fallback.posterUrl,
        containerExtension = data?.containerExtension?.trim()?.trimStart('.')?.ifEmpty { null } ?: fallback.containerExtension,
        rating = Rating.fromRaw(info?.rating) ?: fallback.rating
    )
    return MovieDetails(
        movie = movie,
        plot = info?.plot?.cleanText(),
        cast = info?.cast?.cleanText(),
        director = info?.director?.cleanText(),
        genre = info?.genre?.cleanText(),
        releaseDate = info?.releaseDate ?: info?.releaseDateAlt,
        durationSeconds = info?.durationSeconds ?: DurationParser.parse(info?.duration),
        backdropUrls = info?.backdropPath?.filter { it.startsWith("http") }.orEmpty(),
        trailerUrl = info?.youtubeTrailer?.toYoutubeUrl()
    )
}

fun SeriesInfoResponseDto.toDomain(fallback: Series, json: Json): SeriesDetails {
    val info = info
    val seasonNames = seasons.associate { (it.seasonNumber ?: -1) to (it.name ?: "") }
    val parsedSeasons = episodes.mapNotNull { (key, elements) ->
        val seasonNumber = key.toIntOrNull() ?: return@mapNotNull null
        val episodes = elements.mapNotNull { json.decodeEpisode(it)?.toDomain(fallback.id, seasonNumber) }
        if (episodes.isEmpty()) return@mapNotNull null
        Season(
            number = seasonNumber,
            name = seasonNames[seasonNumber]?.takeIf { it.isNotBlank() } ?: "Season $seasonNumber",
            episodes = episodes.sortedBy { it.episodeNumber }
        )
    }.sortedBy { it.number }

    return SeriesDetails(
        series = fallback.copy(
            posterUrl = info?.cover?.takeIf { it.startsWith("http") } ?: fallback.posterUrl,
            rating = Rating.fromRaw(info?.rating, info?.rating5Based) ?: fallback.rating
        ),
        plot = info?.plot?.cleanText() ?: fallback.plot,
        cast = info?.cast?.cleanText(),
        director = info?.director?.cleanText(),
        genre = info?.genre?.cleanText() ?: fallback.genre,
        releaseDate = info?.releaseDate ?: info?.releaseDateAlt,
        backdropUrls = info?.backdropPath?.filter { it.startsWith("http") }.orEmpty(),
        seasons = parsedSeasons
    )
}

private fun Json.decodeEpisode(element: JsonElement): EpisodeDto? = runCatching { decodeFromJsonElement(EpisodeDto.serializer(), element) }.getOrNull()

private fun EpisodeDto.toDomain(seriesId: Int, seasonFromKey: Int): Episode? {
    val id = id ?: return null
    val seasonNumber = season ?: seasonFromKey
    val episodeNumber = episodeNum ?: 0
    return Episode(
        id = id,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        title = title?.trim()?.ifEmpty { null } ?: "S%02dE%02d".format(seasonNumber, episodeNumber),
        containerExtension = containerExtension?.trim()?.trimStart('.').orEmpty(),
        durationSeconds = info?.durationSeconds ?: DurationParser.parse(info?.duration),
        plot = info?.plot?.cleanText(),
        thumbnailUrl = info?.movieImage?.takeIf { it.startsWith("http") },
        rating = Rating.fromRaw(info?.rating)
    )
}

private fun String.cleanText(): String? = trim().takeUnless { it.isEmpty() || it.equals("n/a", ignoreCase = true) }

private fun String.toYoutubeUrl(): String? {
    val id = trim()
    if (id.isEmpty()) return null
    return if (id.startsWith("http")) id else "https://www.youtube.com/watch?v=$id"
}

/** "01:43:20" or "103 min" or "1h 43m" -> seconds. */
internal object DurationParser {
    private val hms = Regex("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?$")
    private val minutes = Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE)
    private val hm = Regex("(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?", RegexOption.IGNORE_CASE)

    fun parse(raw: String?): Long? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        hms.find(text)?.let { m ->
            val (h, mnt, s) = m.destructured
            return h.toLong() * 3600 + mnt.toLong() * 60 + (s.toLongOrNull() ?: 0)
        }
        minutes.find(text)?.let { return it.groupValues[1].toLong() * 60 }
        hm.find(text)?.takeIf { it.value.isNotBlank() }?.let { m ->
            val h = m.groupValues[1].toLongOrNull() ?: 0
            val mnt = m.groupValues[2].toLongOrNull() ?: 0
            if (h > 0 || mnt > 0) return h * 3600 + mnt * 60
        }
        return text.toLongOrNull()
    }
}
