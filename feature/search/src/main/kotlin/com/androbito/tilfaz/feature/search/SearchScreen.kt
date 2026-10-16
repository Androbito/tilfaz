package com.androbito.tilfaz.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.ChannelRow
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

class SearchNavigation(
    val onPlayChannel: (channelId: Int) -> Unit,
    val onOpenMovie: (movieId: Int) -> Unit,
    val onOpenSeries: (seriesId: Int) -> Unit,
)

fun NavGraphBuilder.searchScreen(navigation: SearchNavigation) {
    composable<SearchRoute> { SearchRoute(navigation) }
}

@Composable
fun SearchRoute(navigation: SearchNavigation, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, viewModel::onQueryChanged, navigation)
}

@Composable
fun SearchScreen(state: SearchUiState, onQueryChanged: (String) -> Unit, navigation: SearchNavigation) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.search_title)}")
        TvTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            label = "",
            placeholder = stringResource(R.string.search_placeholder),
            imeAction = ImeAction.Search,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).fillMaxWidth().focusRequester(focus),
        )
        when {
            state.isTooShort -> EmptyState(title = stringResource(R.string.search_hint, SearchUiState.MIN_QUERY_LENGTH))
            state.isEmpty -> EmptyState(title = stringResource(R.string.search_no_results, state.query))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
                if (state.channels.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_channels), state.channels.size) }
                    items(state.channels.take(MAX_CHANNELS), key = { "c${it.id}" }) { channel ->
                        ChannelRow(channel.name, channel.logoUrl, nowTitle = null, progress = null, selected = false, onClick = { navigation.onPlayChannel(channel.id) })
                    }
                }
                if (state.movies.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_movies), state.movies.size) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = PaddingValues(vertical = Dimens.SpaceS)) {
                            items(state.movies, key = { it.id }) { movie ->
                                PosterCard(movie.name, movie.posterUrl, onClick = { navigation.onOpenMovie(movie.id) }, ratingBadge = movie.rating?.badge)
                            }
                        }
                    }
                }
                if (state.series.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_series), state.series.size) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), contentPadding = PaddingValues(vertical = Dimens.SpaceS)) {
                            items(state.series, key = { it.id }) { series ->
                                PosterCard(series.name, series.posterUrl, onClick = { navigation.onOpenSeries(series.id) }, ratingBadge = series.rating?.badge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, count: Int) {
    Text("$text · $count", style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
}

private const val MAX_CHANNELS = 30
