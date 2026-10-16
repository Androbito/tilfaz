package com.androbito.tilfaz.feature.vod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.RatingBadge
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.formatDuration
import com.androbito.tilfaz.core.designsystem.component.formatPlayerTime
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.Rating
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailsRoute(val movieId: Int) {
    companion object {
        const val ARG_MOVIE_ID = "movieId"
    }
}

fun NavGraphBuilder.movieDetailsScreen(onPlayMovie: (movieId: Int, startPositionMs: Long) -> Unit, onOpenMovie: (movieId: Int) -> Unit) {
    composable<MovieDetailsRoute> { MovieDetailsRoute(onPlayMovie, onOpenMovie) }
}

@Composable
fun MovieDetailsRoute(onPlayMovie: (Int, Long) -> Unit, onOpenMovie: (Int) -> Unit, viewModel: MovieDetailsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MovieDetailsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onPlay = { position -> state.movie?.let { onPlayMovie(it.id, position) } },
        onOpenMovie = onOpenMovie
    )
}

@Composable
fun MovieDetailsScreen(
    state: MovieDetailsUiState,
    onEvent: (MovieDetailsEvent) -> Unit,
    onPlay: (startPositionMs: Long) -> Unit,
    onOpenMovie: (Int) -> Unit
) {
    val movie = state.movie
    if (movie == null) {
        LoadingState()
        return
    }
    val details = state.details
    Box(Modifier.fillMaxSize()) {
        details?.backdropUrls?.firstOrNull()?.let { backdrop ->
            AsyncImage(model = backdrop, contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.18f, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize()) {
            TopBar(title = "Tilfaz", showClock = false)
            Column(
                Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPadding).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXl)) {
                    Box(Modifier.width(280.dp).height(400.dp).clip(RoundedCornerShape(Dimens.CornerM))) {
                        AsyncImage(model = movie.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        movie.rating?.let { RatingBadge(it.badge, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceS)) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                        Text(movie.name, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                        movie.rating?.let { ScoreLine(it) }

                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onPlay(if (state.canResume) state.progress!!.positionMs else 0L) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text(
                                    if (state.canResume) {
                                        stringResource(
                                            R.string.vod_resume,
                                            formatPlayerTime(state.progress!!.positionMs)
                                        )
                                    } else {
                                        stringResource(R.string.vod_play)
                                    },
                                    modifier = Modifier.padding(start = Dimens.SpaceS)
                                )
                            }
                            if (state.canResume) {
                                OutlinedButton(onClick = { onEvent(MovieDetailsEvent.StartOver); onPlay(0L) }) {
                                    Icon(Icons.Default.Replay, contentDescription = null)
                                    Text(stringResource(R.string.vod_start_over), modifier = Modifier.padding(start = Dimens.SpaceS))
                                }
                            }
                            OutlinedButton(onClick = { onEvent(MovieDetailsEvent.ToggleFavorite) }) {
                                Icon(
                                    if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (state.isFavorite) TilfazColors.Danger else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(if (state.isFavorite) R.string.vod_remove_favorite else R.string.vod_add_favorite),
                                    modifier = Modifier.padding(start = Dimens.SpaceS)
                                )
                            }
                        }

                        if (state.detailsFailed) {
                            Text(stringResource(R.string.vod_details_error), color = TilfazColors.Danger, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (details != null) {
                            InfoRow(stringResource(R.string.vod_directed_by), details.director)
                            InfoRow(stringResource(R.string.vod_release_date), details.releaseDate)
                            InfoRow(stringResource(R.string.vod_genre), details.genre)
                            InfoRow(stringResource(R.string.vod_duration), formatDuration(details.durationSeconds))
                            InfoRow(stringResource(R.string.vod_cast), details.cast)
                            InfoRow(stringResource(R.string.vod_plot), details.plot, maxLines = 8)
                        }
                    }
                }
                if (state.related.isNotEmpty()) {
                    Text(stringResource(R.string.vod_related), style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                        contentPadding = PaddingValues(vertical = Dimens.SpaceS, horizontal = Dimens.SpaceS)
                    ) {
                        items(state.related, key = { it.id }) { related ->
                            PosterCard(related.name, related.posterUrl, onClick = { onOpenMovie(related.id) }, ratingBadge = related.rating?.badge)
                        }
                    }
                }
            }
        }
    }
}

/** "7.1 / 10" with filled and empty stars, the way the user is used to seeing scores on detail pages. */
@Composable
private fun ScoreLine(rating: Rating) {
    val filled = rating.outOfFive.toInt().coerceIn(0, 5)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text("★".repeat(filled) + "☆".repeat(5 - filled), style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
        Text(stringResource(R.string.vod_score, rating.outOfTen), style = MaterialTheme.typography.titleMedium, color = TilfazColors.OnSurfaceMuted)
    }
}

@Composable
private fun InfoRow(label: String, value: String?, maxLines: Int = 2) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(180.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = maxLines, modifier = Modifier.weight(1f))
    }
}
