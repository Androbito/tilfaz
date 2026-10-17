package com.androbito.tilfaz.feature.vod

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.androbito.tilfaz.core.designsystem.component.BrowserLayout
import com.androbito.tilfaz.core.designsystem.component.CategoryRow
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.PosterCard
import com.androbito.tilfaz.core.designsystem.component.SuggestionRow
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.domain.search.TitleText
import com.androbito.tilfaz.core.domain.usecase.CatalogBrowser
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
    MoviesScreen(state, viewModel::onEvent, onOpenMovie)
}

@Composable
fun MoviesScreen(state: MoviesUiState, onEvent: (MoviesEvent) -> Unit, onOpenMovie: (Int) -> Unit) {
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
                        CatalogBrowser.Section.Kind.FAVORITES -> stringResource(R.string.vod_favorites) to Icons.Default.Favorite
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
            items = browser.items,
            itemKey = { it.id },
            emptyContent = {
                EmptyState(
                    title = if (browser.query.isNotBlank()) stringResource(R.string.vod_no_results, browser.query) else stringResource(R.string.vod_empty)
                )
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
