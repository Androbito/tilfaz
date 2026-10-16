package com.androbito.tilfaz.core.testing.fixture

import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.Rating
import com.androbito.tilfaz.core.model.Season
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.StreamFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Deterministic test data. The fixed clock is 2026-10-16T20:00:00Z, a Friday evening. */
object Fixtures {
    val now: Instant = Instant.parse("2026-10-16T20:00:00Z")
    val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val account = Account(serverUrl = "http://tv.example.com:8080", username = "ayoub", password = "s3cret")

    val activeStatus = AccountStatus(
        isActive = true,
        expiresAt = Instant.parse("2027-03-07T00:00:00Z"),
        isTrial = false,
        maxConnections = 1,
        activeConnections = 0,
        allowedOutputFormats = listOf(StreamFormat.MPEG_TS, StreamFormat.HLS),
        serverTimeZone = "Europe/Paris",
        checkedAt = now,
    )

    val liveCategories = listOf(
        Category("1", "BEIN SPORTS HD", ContentType.LIVE),
        Category("2", "|AR| NEWS", ContentType.LIVE),
    )

    val alJazeera = LiveChannel(id = 101, number = 1, name = "|AR| AL JAZEERA HD", logoUrl = "http://img/aj.png", categoryId = "2", epgChannelId = "aljazeera.qa")
    val beinSports1 = LiveChannel(id = 102, number = 2, name = "BEIN SPORTS 1 FHD", logoUrl = null, categoryId = "1", epgChannelId = "bein1.fr", hasArchive = true, archiveDays = 3)
    val liveChannels = listOf(alJazeera, beinSports1)

    val movie = Movie(
        id = 501,
        name = "|FR| L'Être Aimé (2026)",
        posterUrl = "http://img/etre.jpg",
        categoryId = "10",
        rating = Rating(7.0),
        year = 2026,
        addedAt = now.minusSeconds(3600),
        containerExtension = "mkv",
    )

    val series = Series(
        id = 901,
        name = "|MULTI| The Torment (2026)",
        posterUrl = "http://img/torment.jpg",
        categoryId = "20",
        rating = Rating(6.0),
        year = 2026,
        lastModifiedAt = now,
    )

    val episode1 = Episode(
        id = "90101",
        seriesId = 901,
        seasonNumber = 1,
        episodeNumber = 1,
        title = "S01E01",
        containerExtension = "mp4",
        durationSeconds = 43 * 60,
        plot = "Kama returns to her hometown.",
        thumbnailUrl = null,
        rating = Rating(8.0),
    )

    val seriesDetails = SeriesDetails(
        series = series,
        plot = "Family secrets, passions and crime.",
        cast = null,
        director = null,
        genre = "Crime, Drama",
        releaseDate = "2026-10-02",
        backdropUrls = emptyList(),
        seasons = listOf(Season(1, "Season 1", listOf(episode1, episode1.copy(id = "90102", episodeNumber = 2, title = "S01E02")))),
    )

    fun epg(channelId: Int, title: String, startOffsetMin: Long, durationMin: Long) = EpgEntry(
        channelId = channelId,
        title = title,
        description = null,
        start = now.plusSeconds(startOffsetMin * 60),
        end = now.plusSeconds((startOffsetMin + durationMin) * 60),
    )
}
