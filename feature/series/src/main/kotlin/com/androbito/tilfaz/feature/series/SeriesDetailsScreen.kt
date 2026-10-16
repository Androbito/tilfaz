package com.androbito.tilfaz.feature.series

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.ErrorState
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.ProgressBar
import com.androbito.tilfaz.core.designsystem.component.RatingBadge
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.formatDuration
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.serialization.Serializable

@Serializable
data class SeriesDetailsRoute(val seriesId: Int) {
    companion object {
        const val ARG_SERIES_ID = "seriesId"
    }
}

fun NavGraphBuilder.seriesDetailsScreen(onPlayEpisode: (seriesId: Int, episodeId: String, startPositionMs: Long) -> Unit) {
    composable<SeriesDetailsRoute> { SeriesDetailsRoute(onPlayEpisode) }
}

@Composable
fun SeriesDetailsRoute(onPlayEpisode: (Int, String, Long) -> Unit, viewModel: SeriesDetailsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeriesDetailsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onPlay = { episode ->
            val progress = state.progressByEpisodeId[episode.id]
            onPlayEpisode(episode.seriesId, episode.id, progress?.takeUnless { it.isFinished }?.positionMs ?: 0L)
        },
    )
}

@Composable
fun SeriesDetailsScreen(state: SeriesDetailsUiState, onEvent: (SeriesDetailsEvent) -> Unit, onPlay: (Episode) -> Unit) {
    val series = state.series
    when {
        state.isLoading && series == null -> { LoadingState(); return }
        state.failed && state.details == null -> {
            ErrorState(stringResource(R.string.series_details_error), onRetry = { onEvent(SeriesDetailsEvent.Retry) }, retryLabel = stringResource(R.string.series_retry))
            return
        }
    }
    if (series == null) return
    val details = state.details

    Box(Modifier.fillMaxSize()) {
        details?.backdropUrls?.firstOrNull()?.let { backdrop ->
            AsyncImage(model = backdrop, contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.18f, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize()) {
            TopBar(title = "Tilfaz", showClock = false)
            LazyColumn(contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceS), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXl)) {
                        Box(Modifier.width(220.dp).height(320.dp).clip(RoundedCornerShape(Dimens.CornerM))) {
                            AsyncImage(model = series.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            series.rating?.let { RatingBadge(it.badge, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceS)) }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                            Text(series.name, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                            if (details != null) {
                                InfoRow(stringResource(R.string.series_directed_by), details.director)
                                InfoRow(stringResource(R.string.series_release_date), details.releaseDate)
                                InfoRow(stringResource(R.string.series_genre), details.genre)
                                InfoRow(stringResource(R.string.series_cast), details.cast)
                                InfoRow(stringResource(R.string.series_plot), details.plot, maxLines = 6)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), verticalAlignment = Alignment.CenterVertically) {
                                state.nextEpisode?.let { next ->
                                    Button(onClick = { onPlay(next) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Text(stringResource(R.string.series_play_episode, next.code), modifier = Modifier.padding(start = Dimens.SpaceS))
                                    }
                                }
                                OutlinedButton(onClick = { onEvent(SeriesDetailsEvent.ToggleFavorite) }) {
                                    Icon(if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (state.isFavorite) TilfazColors.Danger else MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(if (state.isFavorite) R.string.series_remove_favorite else R.string.series_add_favorite), modifier = Modifier.padding(start = Dimens.SpaceS))
                                }
                            }
                        }
                    }
                }
                if (details != null && details.seasons.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                            items(details.seasons, key = { it.number }) { season ->
                                FocusableSurface(
                                    onClick = { onEvent(SeriesDetailsEvent.SelectSeason(season.number)) },
                                    selected = season.number == state.selectedSeason,
                                    corner = Dimens.CornerS,
                                    focusedScale = 1.0f,
                                ) {
                                    Text(stringResource(R.string.series_season, season.number), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS))
                                }
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.series_episodes, state.episodes.size), style = MaterialTheme.typography.titleLarge, color = TilfazColors.OnSurfaceMuted)
                    }
                    items(state.episodes, key = { it.id }) { episode ->
                        EpisodeRow(episode, state.progressByEpisodeId[episode.id], onClick = { onPlay(episode) })
                    }
                } else if (details != null) {
                    item { EmptyState(title = stringResource(R.string.series_no_episodes), modifier = Modifier.height(240.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: WatchProgress?, onClick: () -> Unit) {
    FocusableSurface(onClick = onClick, modifier = Modifier.fillMaxWidth(), corner = Dimens.CornerS, focusedScale = 1.0f) {
        Row(Modifier.fillMaxWidth().padding(Dimens.SpaceM), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(160.dp).height(90.dp).clip(RoundedCornerShape(Dimens.CornerS))) {
                AsyncImage(model = episode.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                if (progress != null) ProgressBar(progress.fraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text("${episode.code}  ${episode.title}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                episode.plot?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                formatDuration(episode.durationSeconds)?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = TilfazColors.Accent) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?, maxLines: Int = 2) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(180.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
