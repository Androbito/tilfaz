package com.androbito.tilfaz.core.model

import java.time.Instant

data class Movie(
    val id: Int,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val rating: Rating?,
    val year: Int?,
    val addedAt: Instant?,
    val containerExtension: String
)

data class MovieDetails(
    val movie: Movie,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val durationSeconds: Long?,
    val backdropUrls: List<String>,
    val trailerUrl: String?
)
