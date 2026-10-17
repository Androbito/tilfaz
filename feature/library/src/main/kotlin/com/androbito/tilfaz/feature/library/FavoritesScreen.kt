package com.androbito.tilfaz.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.ChannelLogo
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.onOptionKey
import com.androbito.tilfaz.core.designsystem.component.rememberFocus
import com.androbito.tilfaz.core.designsystem.component.rememberListFocusMemory
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import kotlinx.serialization.Serializable

@Serializable
data object FavoritesRoute

class FavoritesNavigation(
    val onPlayChannel: (channelId: Int) -> Unit,
    val onOpenMovie: (movieId: Int) -> Unit,
    val onOpenSeries: (seriesId: Int) -> Unit
)

fun NavGraphBuilder.favoritesScreen(navigation: FavoritesNavigation) {
    composable<FavoritesRoute> { FavoritesRoute(navigation) }
}

@Composable
fun FavoritesRoute(navigation: FavoritesNavigation, viewModel: FavoritesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoritesScreen(state = state, onEvent = viewModel::onEvent, navigation = navigation)
}

/** Live, Movies and TV shows, each a horizontal shelf. Hold OK or press Menu on an item to be asked about removing it. */
@Composable
fun FavoritesScreen(state: FavoritesUiState, onEvent: (FavoritesEvent) -> Unit, navigation: FavoritesNavigation) {
    val itemCount = state.channels.size + state.movies.size + state.series.size
    // One flat index over the three shelves, so a single memory covers initial focus, Back from the player and removal.
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    val movieOffset = state.channels.size
    val seriesOffset = movieOffset + state.movies.size
    // Resolved here: the shelf block below is not composable.
    val liveTitle = stringResource(R.string.favorites_live)
    val moviesTitle = stringResource(R.string.favorites_movies)
    val seriesTitle = stringResource(R.string.favorites_series)
    val liveEmpty = stringResource(R.string.favorites_empty_live)
    val moviesEmpty = stringResource(R.string.favorites_empty_movies)
    val seriesEmpty = stringResource(R.string.favorites_empty_series)

    LibraryPage(
        title = stringResource(R.string.favorites_title),
        hint = stringResource(R.string.favorites_hint),
        itemCount = itemCount,
        focusMemory = focusMemory,
        confirmation = state.pendingRemoval?.let {
            Confirmation(
                title = stringResource(R.string.favorites_remove_title),
                message = stringResource(R.string.favorites_remove_message, it.name),
                confirmLabel = stringResource(R.string.favorites_remove_confirm),
                cancelLabel = stringResource(R.string.favorites_remove_cancel)
            )
        },
        onConfirm = { onEvent(FavoritesEvent.ConfirmRemoval) },
        onCancel = { onEvent(FavoritesEvent.CancelRemoval) }
    ) {
        shelf(liveTitle, liveEmpty, state.channels.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.channels, key = { _, channel -> channel.id }) { index, channel ->
                    val ask = { onEvent(FavoritesEvent.RequestRemoval(ContentType.LIVE, channel.id, channel.name)) }
                    ChannelCard(
                        channel = channel,
                        onClick = { navigation.onPlayChannel(channel.id) },
                        onLongClick = ask,
                        modifier = Modifier.rememberFocus(focusMemory, index).onOptionKey(ask)
                    )
                }
            }
        }
        shelf(moviesTitle, moviesEmpty, state.movies.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.movies, key = { _, movie -> movie.id }) { index, movie ->
                    val ask = { onEvent(FavoritesEvent.RequestRemoval(ContentType.VOD, movie.id, movie.name)) }
                    PosterCard(
                        title = movie.name,
                        posterUrl = movie.posterUrl,
                        onClick = { navigation.onOpenMovie(movie.id) },
                        onLongClick = ask,
                        ratingBadge = movie.rating?.badge,
                        modifier = Modifier.width(PosterWidth).rememberFocus(focusMemory, movieOffset + index).onOptionKey(ask)
                    )
                }
            }
        }
        shelf(seriesTitle, seriesEmpty, state.series.isEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = ShelfPadding) {
                itemsIndexed(state.series, key = { _, series -> series.id }) { index, series ->
                    val ask = { onEvent(FavoritesEvent.RequestRemoval(ContentType.SERIES, series.id, series.name)) }
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

/** Room for the focus ring and the zoom of a focused card, so the first and last cards are not clipped. */
private val ShelfPadding = PaddingValues(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceM)

// Narrower than the 180dp poster of the browsers: three shelves have to share a 540dp-tall screen.
private val PosterWidth = 132.dp

@Composable
private fun ChannelCard(channel: LiveChannel, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(PosterWidth + 16.dp).height(ChannelCardHeight),
        corner = Dimens.CornerM,
        focusedScale = 1.08f
    ) {
        Column(
            Modifier.fillMaxWidth().padding(Dimens.SpaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
        ) {
            ChannelLogo(channel.logoUrl, Modifier.size(72.dp))
            Text(
                channel.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val ChannelCardHeight = 152.dp
