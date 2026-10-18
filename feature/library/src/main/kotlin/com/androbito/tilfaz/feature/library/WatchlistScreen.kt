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
import com.androbito.tilfaz.core.model.ContentType
import kotlinx.serialization.Serializable

@Serializable
data object WatchlistRoute

class WatchlistNavigation(
    val onOpenMovie: (movieId: Int) -> Unit,
    val onOpenSeries: (seriesId: Int) -> Unit
)

fun NavGraphBuilder.watchlistScreen(navigation: WatchlistNavigation) {
    composable<WatchlistRoute> { WatchlistRoute(navigation) }
}

@Composable
fun WatchlistRoute(navigation: WatchlistNavigation, viewModel: WatchlistViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WatchlistScreen(state = state, onEvent = viewModel::onEvent, navigation = navigation)
}

/** Movies and TV shows the user plans to watch, each a horizontal shelf. Hold OK or press Menu on an item to remove it. */
@Composable
fun WatchlistScreen(state: WatchlistUiState, onEvent: (WatchlistEvent) -> Unit, navigation: WatchlistNavigation) {
    val itemCount = state.movies.size + state.series.size
    // One flat index over the two shelves, so a single memory covers initial focus, Back from a page and removal.
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    val seriesOffset = state.movies.size
    // Resolved here: the shelf block below is not composable.
    val moviesTitle = stringResource(R.string.watchlist_movies)
    val seriesTitle = stringResource(R.string.watchlist_series)
    val moviesEmpty = stringResource(R.string.watchlist_empty_movies)
    val seriesEmpty = stringResource(R.string.watchlist_empty_series)

    LibraryPage(
        title = stringResource(R.string.watchlist_title),
        hint = stringResource(R.string.watchlist_hint),
        itemCount = itemCount,
        focusMemory = focusMemory,
        confirmation = state.pendingRemoval?.let {
            Confirmation(
                title = stringResource(R.string.watchlist_remove_title),
                message = stringResource(R.string.watchlist_remove_message, it.name),
                confirmLabel = stringResource(R.string.watchlist_remove_confirm),
                cancelLabel = stringResource(R.string.watchlist_remove_cancel)
            )
        },
        onConfirm = { onEvent(WatchlistEvent.ConfirmRemoval) },
        onCancel = { onEvent(WatchlistEvent.CancelRemoval) }
    ) {
        shelf(moviesTitle, moviesEmpty, state.movies.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.movies, key = { _, movie -> movie.id }) { index, movie ->
                    val ask = { onEvent(WatchlistEvent.RequestRemoval(ContentType.VOD, movie.id, movie.name)) }
                    PosterCard(
                        title = movie.name,
                        posterUrl = movie.posterUrl,
                        onClick = { navigation.onOpenMovie(movie.id) },
                        onLongClick = ask,
                        ratingBadge = movie.rating?.badge,
                        modifier = Modifier.width(PosterWidth).rememberFocus(focusMemory, index).onOptionKey(ask)
                    )
                }
            }
        }
        shelf(seriesTitle, seriesEmpty, state.series.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.series, key = { _, series -> series.id }) { index, series ->
                    val ask = { onEvent(WatchlistEvent.RequestRemoval(ContentType.SERIES, series.id, series.name)) }
                    PosterCard(
                        title = series.name,
                        posterUrl = series.posterUrl,
                        onClick = { navigation.onOpenSeries(series.id) },
                        onLongClick = ask,
                        ratingBadge = series.rating?.badge,
                        modifier = Modifier.width(PosterWidth).rememberFocus(focusMemory, seriesOffset + index).onOptionKey(ask)
                    )
                }
            }
        }
    }
}
