package com.androbito.tilfaz.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.onOptionKey
import com.androbito.tilfaz.core.designsystem.component.rememberFocus
import com.androbito.tilfaz.core.designsystem.component.rememberListFocusMemory
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import kotlinx.serialization.Serializable

@Serializable
data object ContinueWatchingRoute

class ContinueWatchingNavigation(
    val onPlayMovie: (movieId: Int, startPositionMs: Long) -> Unit,
    val onPlayEpisode: (seriesId: Int, episodeId: String, startPositionMs: Long) -> Unit
)

fun NavGraphBuilder.continueWatchingScreen(navigation: ContinueWatchingNavigation) {
    composable<ContinueWatchingRoute> { ContinueWatchingRoute(navigation) }
}

@Composable
fun ContinueWatchingRoute(navigation: ContinueWatchingNavigation, viewModel: ContinueWatchingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ContinueWatchingScreen(state = state, onEvent = viewModel::onEvent, navigation = navigation)
}

/** Movies and TV shows in progress, each a horizontal shelf. OK resumes; hold OK or press Menu to be asked about marking it watched. */
@Composable
fun ContinueWatchingScreen(state: ContinueWatchingUiState, onEvent: (ContinueWatchingEvent) -> Unit, navigation: ContinueWatchingNavigation) {
    val itemCount = state.movies.size + state.shows.size
    // One flat index over both shelves, so a single memory covers initial focus, Back from the player and marking.
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    val showOffset = state.movies.size
    // Resolved here: the shelf block below is not composable.
    val moviesTitle = stringResource(R.string.continue_movies)
    val seriesTitle = stringResource(R.string.continue_series)
    val moviesEmpty = stringResource(R.string.continue_empty_movies)
    val seriesEmpty = stringResource(R.string.continue_empty_series)

    LibraryPage(
        title = stringResource(R.string.continue_title),
        hint = stringResource(R.string.continue_hint),
        itemCount = itemCount,
        focusMemory = focusMemory,
        confirmation = state.pendingWatched?.let {
            Confirmation(
                title = stringResource(R.string.continue_mark_title),
                message = stringResource(R.string.continue_mark_message, it.name),
                confirmLabel = stringResource(R.string.continue_mark_confirm),
                cancelLabel = stringResource(R.string.continue_mark_cancel)
            )
        },
        onConfirm = { onEvent(ContinueWatchingEvent.ConfirmMarkWatched) },
        onCancel = { onEvent(ContinueWatchingEvent.CancelMarkWatched) }
    ) {
        shelf(moviesTitle, moviesEmpty, state.movies.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.movies, key = { _, item -> item.movie.id }) { index, item ->
                    val ask = { onEvent(ContinueWatchingEvent.RequestMarkWatched(item.progress, item.movie.name)) }
                    PosterCard(
                        title = item.movie.name,
                        posterUrl = item.movie.posterUrl,
                        onClick = { navigation.onPlayMovie(item.movie.id, item.progress.positionMs) },
                        onLongClick = ask,
                        ratingBadge = item.movie.rating?.badge,
                        progress = item.progress.fraction,
                        modifier = Modifier.width(PosterWidth).rememberFocus(focusMemory, index).onOptionKey(ask)
                    )
                }
            }
        }
        shelf(seriesTitle, seriesEmpty, state.shows.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.shows, key = { _, item -> item.series.id }) { index, item ->
                    val ask = { onEvent(ContinueWatchingEvent.RequestMarkWatched(item.progress, item.series.name)) }
                    PosterCard(
                        title = item.series.name,
                        posterUrl = item.series.posterUrl,
                        onClick = { navigation.onPlayEpisode(item.series.id, item.episodeId, item.progress.positionMs) },
                        onLongClick = ask,
                        ratingBadge = item.series.rating?.badge,
                        progress = item.progress.fraction,
                        modifier = Modifier.width(PosterWidth).rememberFocus(focusMemory, showOffset + index).onOptionKey(ask)
                    )
                }
            }
        }
    }
}
