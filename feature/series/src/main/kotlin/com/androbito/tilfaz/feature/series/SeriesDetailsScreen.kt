package com.androbito.tilfaz.feature.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.ErrorState
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.component.ListFocusMemory
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.PlotRow
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.ProgressBar
import com.androbito.tilfaz.core.designsystem.component.ProvideMinimalBringIntoView
import com.androbito.tilfaz.core.designsystem.component.RatingBadge
import com.androbito.tilfaz.core.designsystem.component.ScoreLine
import com.androbito.tilfaz.core.designsystem.component.TilfazButton
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.backKeyAsBackPress
import com.androbito.tilfaz.core.designsystem.component.formatDuration
import com.androbito.tilfaz.core.designsystem.component.rememberFocus
import com.androbito.tilfaz.core.designsystem.component.rememberListFocusMemory
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.Rating
import com.androbito.tilfaz.core.model.Season
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.WatchProgress
import kotlinx.serialization.Serializable

@Serializable
data class SeriesDetailsRoute(val seriesId: Int) {
    companion object {
        const val ARG_SERIES_ID = "seriesId"
    }
}

fun NavGraphBuilder.seriesDetailsScreen(
    onPlayEpisode: (seriesId: Int, episodeId: String, startPositionMs: Long) -> Unit,
    onOpenSeries: (seriesId: Int) -> Unit
) {
    composable<SeriesDetailsRoute> { SeriesDetailsRoute(onPlayEpisode, onOpenSeries) }
}

@Composable
fun SeriesDetailsRoute(onPlayEpisode: (Int, String, Long) -> Unit, onOpenSeries: (Int) -> Unit, viewModel: SeriesDetailsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SeriesDetailsRoute(
        state = state,
        onEvent = viewModel::onEvent,
        onPlayEpisode = onPlayEpisode,
        onOpenSeries = onOpenSeries
    )
}

@Composable
fun SeriesDetailsRoute(
    state: SeriesDetailsUiState,
    onEvent: (SeriesDetailsEvent) -> Unit,
    onPlayEpisode: (Int, String, Long) -> Unit,
    onOpenSeries: (Int) -> Unit
) {
    SeriesDetailsScreen(
        state = state,
        onEvent = onEvent,
        onPlay = { episode ->
            val progress = state.progressByEpisodeId[episode.id]
            onPlayEpisode(episode.seriesId, episode.id, progress?.takeUnless { it.isFinished }?.positionMs ?: 0L)
        },
        onOpenSeries = onOpenSeries
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeriesDetailsScreen(state: SeriesDetailsUiState, onEvent: (SeriesDetailsEvent) -> Unit, onPlay: (Episode) -> Unit, onOpenSeries: (Int) -> Unit) {
    val series = state.series
    when {
        state.isLoading && series == null -> { LoadingState(); return }
        state.failed && state.details == null -> {
            ErrorState(stringResource(R.string.series_details_error), onRetry = {
                onEvent(SeriesDetailsEvent.Retry)
            }, retryLabel = stringResource(R.string.series_retry))
            return
        }
    }
    if (series == null) return
    val details = state.details
    var episodesOpen by rememberSaveable { mutableStateOf(false) }
    val playFocus = remember { FocusRequester() }
    val episodesButtonFocus = remember { FocusRequester() }
    val nextEpisodeFocus = remember { FocusRequester() }
    val episodeFocus = rememberListFocusMemory()
    // A fresh open puts focus on the episode that would play next; a restore after the player is left to episodeFocus.
    var focusNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeRowReady by remember { mutableStateOf(false) }
    var focusEpisodesButton by remember { mutableStateOf(false) }

    // Play takes focus on a fresh open of the page. After returning from the player with the panel open it must not.
    LaunchedEffect(state.nextEpisode != null) {
        if (state.nextEpisode != null && !episodesOpen) {
            withFrameNanos { } // the button joins the focus tree once its first frame is laid out
            playFocus.requestFocus()
        }
    }
    LaunchedEffect(focusNextEpisode, nextEpisodeRowReady) {
        if (focusNextEpisode && nextEpisodeRowReady) {
            nextEpisodeFocus.requestFocus()
            focusNextEpisode = false
        }
    }
    LaunchedEffect(focusEpisodesButton) {
        if (focusEpisodesButton) {
            withFrameNanos { } // the page becomes focusable again once the panel is gone
            episodesButtonFocus.requestFocus()
            focusEpisodesButton = false
        }
    }
    BackHandler(enabled = episodesOpen) {
        episodesOpen = false
        nextEpisodeRowReady = false
        focusEpisodesButton = true
    }

    ProvideMinimalBringIntoView {
        Box(Modifier.fillMaxSize().backKeyAsBackPress()) {
            details?.backdropUrls?.firstOrNull()?.let { backdrop ->
                AsyncImage(model = backdrop, contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.18f, modifier = Modifier.fillMaxSize())
            }
            // While the panel is open the page behind it must not take focus, or Left would walk out of the panel.
            Column(Modifier.fillMaxSize().focusProperties { canFocus = !episodesOpen }) {
                TopBar(title = "Tilfaz", showClock = false)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceS),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXl)) {
                            Box(Modifier.width(220.dp).height(320.dp).clip(RoundedCornerShape(Dimens.CornerM))) {
                                AsyncImage(
                                    model = series.posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                series.rating?.let { RatingBadge(it.badge, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceS)) }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                                Text(series.name, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                                series.rating?.let { ScoreLine(it) }
                                if (details != null) {
                                    InfoRow(stringResource(R.string.series_directed_by), details.director)
                                    InfoRow(stringResource(R.string.series_release_date), details.releaseDate)
                                    InfoRow(stringResource(R.string.series_genre), details.genre)
                                    InfoRow(stringResource(R.string.series_cast), details.cast)
                                    PlotRow(stringResource(R.string.series_plot), details.plot)
                                }
                                // Wraps onto a second line: four buttons with these labels are wider than the column.
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    state.nextEpisode?.let { next ->
                                        TilfazButton(onClick = { onPlay(next) }, modifier = Modifier.focusRequester(playFocus)) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Text(stringResource(R.string.series_play_episode, next.code), modifier = Modifier.padding(start = Dimens.SpaceS))
                                        }
                                    }
                                    if (details != null && details.seasons.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                state.nextEpisode?.let { onEvent(SeriesDetailsEvent.SelectSeason(it.seasonNumber)) }
                                                episodesOpen = true
                                                focusNextEpisode = true
                                            },
                                            modifier = Modifier.focusRequester(episodesButtonFocus)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                            Text(stringResource(R.string.series_show_episodes), modifier = Modifier.padding(start = Dimens.SpaceS))
                                        }
                                    }
                                    OutlinedButton(onClick = { onEvent(SeriesDetailsEvent.ToggleFavorite) }) {
                                        Icon(
                                            if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (state.isFavorite) TilfazColors.Danger else LocalContentColor.current
                                        )
                                        Text(
                                            stringResource(if (state.isFavorite) R.string.series_remove_favorite else R.string.series_add_favorite),
                                            modifier = Modifier.padding(start = Dimens.SpaceS)
                                        )
                                    }
                                    OutlinedButton(onClick = { onEvent(SeriesDetailsEvent.ToggleWatchlist) }) {
                                        Icon(if (state.isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null)
                                        Text(
                                            stringResource(if (state.isInWatchlist) R.string.series_remove_watchlist else R.string.series_add_watchlist),
                                            modifier = Modifier.padding(start = Dimens.SpaceS)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (details != null && details.seasons.isEmpty()) {
                        item { EmptyState(title = stringResource(R.string.series_no_episodes), modifier = Modifier.height(240.dp)) }
                    }
                    if (state.related.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.series_related), style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                                contentPadding = PaddingValues(vertical = Dimens.SpaceS, horizontal = Dimens.SpaceS)
                            ) {
                                items(state.related, key = { it.id }) { related ->
                                    PosterCard(related.name, related.posterUrl, onClick = { onOpenSeries(related.id) }, ratingBadge = related.rating?.badge)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = episodesOpen, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(TilfazColors.Scrim))
            }
            AnimatedVisibility(
                visible = episodesOpen,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.fillMaxSize()
            ) {
                Row(Modifier.fillMaxSize()) {
                    Spacer(Modifier.weight(1f - PANEL_WIDTH_FRACTION))
                    EpisodesPanel(
                        state = state,
                        seasons = details?.seasons.orEmpty(),
                        episodeFocus = episodeFocus,
                        nextEpisodeFocus = nextEpisodeFocus,
                        onNextEpisodeRowReady = { nextEpisodeRowReady = true },
                        onPlay = onPlay,
                        onEvent = onEvent,
                        modifier = Modifier.weight(PANEL_WIDTH_FRACTION).fillMaxHeight()
                    )
                }
            }
        }
    }
}

private const val PANEL_WIDTH_FRACTION = 0.58f

@Composable
private fun EpisodesPanel(
    state: SeriesDetailsUiState,
    seasons: List<Season>,
    episodeFocus: ListFocusMemory,
    nextEpisodeFocus: FocusRequester,
    onNextEpisodeRowReady: () -> Unit,
    onPlay: (Episode) -> Unit,
    onEvent: (SeriesDetailsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextEpisodeId = state.nextEpisode?.id
    Column(
        modifier.background(TilfazColors.Surface).padding(Dimens.SpaceL).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Text(stringResource(R.string.series_episodes, state.episodes.size), style = MaterialTheme.typography.titleLarge, color = TilfazColors.OnSurfaceMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            items(seasons, key = { it.number }) { season ->
                FocusableSurface(
                    onClick = { onEvent(SeriesDetailsEvent.SelectSeason(season.number)) },
                    selected = season.number == state.selectedSeason,
                    corner = Dimens.CornerS,
                    focusedScale = 1.0f
                ) {
                    Text(
                        stringResource(R.string.series_season, season.number),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS)
                    )
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
            itemsIndexed(state.episodes, key = { _, episode -> episode.id }) { index, episode ->
                val isNext = episode.id == nextEpisodeId
                EpisodeRow(
                    episode = episode,
                    progress = state.progressByEpisodeId[episode.id],
                    isNext = isNext,
                    onClick = { onPlay(episode) },
                    modifier = Modifier
                        .rememberFocus(episodeFocus, index)
                        .then(if (isNext) Modifier.focusRequester(nextEpisodeFocus).onGloballyPositioned { onNextEpisodeRowReady() } else Modifier)
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: WatchProgress?, isNext: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusableSurface(onClick = onClick, modifier = modifier.fillMaxWidth(), corner = Dimens.CornerS, focusedScale = 1.0f) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.SpaceM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(128.dp).height(72.dp).clip(RoundedCornerShape(Dimens.CornerS)).background(TilfazColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Shown while the thumbnail loads and whenever the panel has none.
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TilfazColors.OnSurfaceMuted, modifier = Modifier.size(32.dp))
                AsyncImage(model = episode.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                if (progress != null && !progress.isFinished) {
                    ProgressBar(progress.fraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(
                    episode.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.plot?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                    formatDuration(episode.durationSeconds)?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = TilfazColors.Accent) }
                    when {
                        progress?.isFinished == true ->
                            Text(stringResource(R.string.series_watched), style = MaterialTheme.typography.labelLarge, color = TilfazColors.Success)
                        isNext ->
                            Text(stringResource(R.string.series_next_up), style = MaterialTheme.typography.labelLarge, color = TilfazColors.OnBackground)
                    }
                }
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

@Preview
@Composable
private fun SeriesDetailsRoutePreview() {
    TilfazTheme {
        SeriesDetailsRoute(
            state = sampleUiState,
            onEvent = {},
            onPlayEpisode = { _, _, _ -> },
            onOpenSeries = {}
        )
    }
}

private val sampleSeries = Series(
    id = 901,
    name = "The Torment",
    posterUrl = "http://img/torment.jpg",
    categoryId = "20",
    rating = Rating(8.5),
    year = 2026,
    lastModifiedAt = null
)

private val sampleEpisode1 = Episode(
    id = "90101",
    seriesId = 901,
    seasonNumber = 1,
    episodeNumber = 1,
    title = "Pilot",
    containerExtension = "mp4",
    durationSeconds = 2700,
    plot = "Kama returns to her hometown and discovers a mystery.",
    thumbnailUrl = null,
    rating = Rating(8.0)
)

private val sampleEpisode2 = Episode(
    id = "90102",
    seriesId = 901,
    seasonNumber = 1,
    episodeNumber = 2,
    title = "The Reckoning",
    containerExtension = "mp4",
    durationSeconds = 2820,
    plot = "Secrets unfold as the investigation deepens.",
    thumbnailUrl = null,
    rating = Rating(8.2)
)

private val sampleSeriesDetails = SeriesDetails(
    series = sampleSeries,
    plot = "Family secrets, passions and crime converge in a small coastal town.",
    cast = "Actor One, Actor Two, Actor Three",
    director = "Director Name",
    genre = "Crime, Drama, Thriller",
    releaseDate = "2026-10-02",
    backdropUrls = emptyList(),
    seasons = listOf(
        Season(number = 1, name = "Season 1", episodes = listOf(sampleEpisode1, sampleEpisode2))
    )
)

private val sampleUiState = SeriesDetailsUiState(
    series = sampleSeries,
    details = sampleSeriesDetails,
    isLoading = false,
    failed = false,
    isFavorite = true,
    selectedSeason = 1
)
