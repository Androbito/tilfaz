package com.androbito.tilfaz.feature.vod

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.androbito.tilfaz.core.designsystem.component.BrowserLayout
import com.androbito.tilfaz.core.designsystem.component.CategoryRow
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.SuggestionRow
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.TitleText
import kotlinx.serialization.Serializable

@Serializable
data class MoviesRoute(val initialSectionId: String? = null) {
    companion object {
        const val ARG_INITIAL_SECTION = "initialSectionId"
    }
}

fun NavGraphBuilder.moviesScreen(onOpenMovie: (movieId: Int) -> Unit) {
    composable<MoviesRoute> { MoviesRoute(onOpenMovie) }
}

@Composable
fun MoviesRoute(onOpenMovie: (Int) -> Unit, viewModel: MoviesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    MoviesScreen(state, items, viewModel::onEvent, onOpenMovie)
}

@Composable
fun MoviesScreen(state: MoviesUiState, items: LazyPagingItems<Movie>, onEvent: (MoviesEvent) -> Unit, onOpenMovie: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.vod_title)}")
        val browser = state.browser
        if (browser == null) {
            LoadingState()
            return
        }
        BrowserLayout(
            query = browser.query,
            onQueryChange = { onEvent(MoviesEvent.QueryChanged(it)) },
            searchPlaceholder = stringResource(R.string.vod_search),
            sections = {
                items(browser.sections, key = { it.id }) { section ->
                    val (name, icon) = when (section.kind) {
                        CatalogBrowser.Section.Kind.ALL -> stringResource(R.string.vod_all) to Icons.Default.ViewList
                        CatalogBrowser.Section.Kind.CONTINUE_WATCHING -> stringResource(R.string.vod_continue) to Icons.Default.History
                        CatalogBrowser.Section.Kind.RECENTLY_ADDED -> stringResource(R.string.vod_recent) to Icons.Default.NewReleases
                        CatalogBrowser.Section.Kind.PROVIDER -> section.name to null
                    }
                    CategoryRow(name, section.count, section.id == browser.selectedSectionId, { onEvent(MoviesEvent.SelectSection(section.id)) }, icon = icon)
                }
            },
            suggestions = {
                items(browser.suggestions, key = { it.id }) { movie ->
                    SuggestionRow(TitleText.clean(movie.name), onClick = { onOpenMovie(movie.id) })
                }
            },
            itemCount = { items.itemCount },
            itemAt = { items[it] },
            itemKey = items.itemKey { it.id },
            emptyContent = {
                // While the first page loads the grid is empty too, and that is not "no results".
                if (items.loadState.refresh !is LoadState.Loading) {
                    EmptyState(
                        title = if (browser.query.isNotBlank()) stringResource(R.string.vod_no_results, browser.query) else stringResource(R.string.vod_empty)
                    )
                }
            }
        ) { movie ->
            PosterCard(
                title = movie.name,
                posterUrl = movie.posterUrl,
                onClick = { onOpenMovie(movie.id) },
                onLongClick = { onEvent(MoviesEvent.ToggleFavorite(movie.id)) },
                ratingBadge = movie.rating?.badge,
                isFavorite = movie.id in browser.favoriteIds,
                progress = browser.progressById[movie.id]?.fraction
            )
        }
    }
}
