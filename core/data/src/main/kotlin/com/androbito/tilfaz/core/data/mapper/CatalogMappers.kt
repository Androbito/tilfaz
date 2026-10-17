package com.androbito.tilfaz.core.data.mapper

import com.androbito.tilfaz.core.database.entity.CategoryEntity
import com.androbito.tilfaz.core.database.entity.LiveChannelEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.SeriesEntity
import com.androbito.tilfaz.core.database.entity.SyncStatusEntity
import com.androbito.tilfaz.core.model.Category
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.Rating
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.model.TitleText
import com.androbito.tilfaz.core.network.dto.CategoryDto
import com.androbito.tilfaz.core.network.dto.LiveStreamDto
import com.androbito.tilfaz.core.network.dto.SeriesDto
import com.androbito.tilfaz.core.network.dto.VodStreamDto
import java.time.Instant

/*
 * DTO -> entity happens at sync time, entity -> domain at read time. Rows the provider sent without an id are
 * dropped rather than stored under a fake id: they could never be played anyway.
 */

fun List<CategoryDto>.toEntities(type: ContentType): List<CategoryEntity> = mapIndexedNotNull { index, dto ->
    val id = dto.categoryId ?: return@mapIndexedNotNull null
    CategoryEntity(id = id, type = type.name, name = dto.categoryName ?: "Category $id", parentId = dto.parentId, position = index)
}

fun CategoryEntity.toDomain() = Category(id = id, name = name, type = ContentType.valueOf(type), parentId = parentId)

/** [startIndex] is where this chunk starts in the whole list, so positions stay the provider's order across chunks. */
fun List<LiveStreamDto>.toLiveEntities(startIndex: Int = 0): List<LiveChannelEntity> = mapIndexedNotNull { index, dto ->
    val id = dto.streamId ?: return@mapIndexedNotNull null
    val name = dto.name?.trim().orEmpty().ifEmpty { "Channel $id" }
    LiveChannelEntity(
        id = id,
        number = dto.num,
        name = name,
        nameNormalized = TitleText.searchKey(name),
        logoUrl = dto.streamIcon?.takeIf { it.startsWith("http") },
        categoryId = dto.categoryId,
        epgChannelId = dto.epgChannelId,
        hasArchive = dto.tvArchive ?: false,
        archiveDays = dto.tvArchiveDuration ?: 0,
        position = startIndex + index
    )
}

fun LiveChannelEntity.toDomain() = LiveChannel(
    id = id,
    number = number,
    name = name,
    logoUrl = logoUrl,
    categoryId = categoryId,
    epgChannelId = epgChannelId,
    hasArchive = hasArchive,
    archiveDays = archiveDays
)

fun List<VodStreamDto>.toMovieEntities(startIndex: Int = 0): List<MovieEntity> = mapIndexedNotNull { index, dto ->
    val id = dto.streamId ?: return@mapIndexedNotNull null
    val name = dto.name?.trim().orEmpty().ifEmpty { "Movie $id" }
    MovieEntity(
        id = id,
        name = name,
        nameNormalized = TitleText.searchKey(name),
        posterUrl = dto.streamIcon?.takeIf { it.startsWith("http") },
        categoryId = dto.categoryId,
        ratingOutOfTen = Rating.fromRaw(dto.rating, dto.rating5Based)?.outOfTen,
        year = dto.year ?: YearExtractor.fromTitle(name) ?: YearExtractor.fromDate(dto.releaseDate),
        addedAtMillis = dto.added?.toEpochMillis(),
        containerExtension = dto.containerExtension?.trim()?.trimStart('.').orEmpty(),
        position = startIndex + index
    )
}

fun MovieEntity.toDomain() = Movie(
    id = id,
    name = name,
    posterUrl = posterUrl,
    categoryId = categoryId,
    rating = ratingOutOfTen?.let(::Rating),
    year = year,
    addedAt = addedAtMillis?.let(Instant::ofEpochMilli),
    containerExtension = containerExtension
)

fun List<SeriesDto>.toSeriesEntities(startIndex: Int = 0): List<SeriesEntity> = mapIndexedNotNull { index, dto ->
    val id = dto.seriesId ?: return@mapIndexedNotNull null
    val name = dto.name?.trim().orEmpty().ifEmpty { "Series $id" }
    SeriesEntity(
        id = id,
        name = name,
        nameNormalized = TitleText.searchKey(name),
        posterUrl = dto.cover?.takeIf { it.startsWith("http") },
        categoryId = dto.categoryId,
        ratingOutOfTen = Rating.fromRaw(dto.rating, dto.rating5Based)?.outOfTen,
        year = YearExtractor.fromDate(dto.releaseDate ?: dto.releaseDateAlt) ?: YearExtractor.fromTitle(name),
        lastModifiedMillis = dto.lastModified?.toEpochMillis(),
        // Only the fallback for a details page that cannot be fetched: the full text comes with the details.
        plot = dto.plot?.take(PLOT_LIMIT),
        genre = dto.genre?.take(GENRE_LIMIT),
        position = startIndex + index
    )
}

fun SeriesEntity.toDomain() = Series(
    id = id,
    name = name,
    posterUrl = posterUrl,
    categoryId = categoryId,
    rating = ratingOutOfTen?.let(::Rating),
    year = year,
    lastModifiedAt = lastModifiedMillis?.let(Instant::ofEpochMilli),
    plot = plot,
    genre = genre
)

fun SyncStatusEntity?.toDomain(type: ContentType, isSyncing: Boolean) = SyncStatus(
    contentType = type,
    lastSyncedAt = this?.lastSyncedAtMillis?.let(Instant::ofEpochMilli),
    itemCount = this?.itemCount ?: 0,
    isSyncing = isSyncing
)

/** Xtream timestamps are unix seconds; a few panels already send milliseconds. */
internal fun Long.toEpochMillis(): Long = if (this > 100_000_000_000L) this else this * 1000

internal object YearExtractor {
    private val titleYear = Regex("\\((19|20)\\d{2}\\)")
    private val anyYear = Regex("(19|20)\\d{2}")

    fun fromTitle(title: String): Int? = titleYear.find(title)?.value?.trim('(', ')')?.toIntOrNull()
    fun fromDate(date: String?): Int? = date?.let { anyYear.find(it)?.value?.toIntOrNull() }
}

private const val PLOT_LIMIT = 400
private const val GENRE_LIMIT = 100
