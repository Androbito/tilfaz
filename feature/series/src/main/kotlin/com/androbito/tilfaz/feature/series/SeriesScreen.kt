package com.androbito.tilfaz.feature.series

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
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.TitleText
import kotlinx.serialization.Serializable

@Serializable
data object SeriesRoute

fun NavGraphBuilder.seriesScreen(onOpenSeries: (seriesId: Int) -> Unit) {
    composable<SeriesRoute> { SeriesRoute(onOpenSeries) }
}

@Composable
fun SeriesRoute(onOpenSeries: (Int) -> Unit, viewModel: SeriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    SeriesScreen(state, items, viewModel::onEvent, onOpenSeries)
}

@Composable
fun SeriesScreen(state: SeriesUiState, items: LazyPagingItems<Series>, onEvent: (SeriesEvent) -> Unit, onOpenSeries: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.series_title)}")
        val browser = state.browser
        if (browser == null) {
            LoadingState()
            return
        }
        BrowserLayout(
            query = browser.query,
            onQueryChange = { onEvent(SeriesEvent.QueryChanged(it)) },
            searchPlaceholder = stringResource(R.string.series_search),
            sections = {
                items(browser.sections, key = { it.id }) { section ->
                    val (name, icon) = when (section.kind) {
                        CatalogBrowser.Section.Kind.ALL -> stringResource(R.string.series_all) to Icons.Default.ViewList
                        CatalogBrowser.Section.Kind.CONTINUE_WATCHING -> stringResource(R.string.series_continue) to Icons.Default.History
                        CatalogBrowser.Section.Kind.RECENTLY_ADDED -> stringResource(R.string.series_recent) to Icons.Default.NewReleases
                        CatalogBrowser.Section.Kind.PROVIDER -> section.name to null
                    }
                    CategoryRow(name, section.count, section.id == browser.selectedSectionId, { onEvent(SeriesEvent.SelectSection(section.id)) }, icon = icon)
                }
            },
            suggestions = {
                items(browser.suggestions, key = { it.id }) { series ->
                    SuggestionRow(TitleText.clean(series.name), onClick = { onOpenSeries(series.id) })
                }
            },
            itemCount = { items.itemCount },
            itemAt = { items[it] },
            itemKey = items.itemKey { it.id },
            emptyContent = {
                // While the first page loads the grid is empty too, and that is not "no results".
                if (items.loadState.refresh !is LoadState.Loading) {
                    EmptyState(
                        title = if (browser.query.isNotBlank()) {
                            stringResource(
                                R.string.series_no_results,
                                browser.query
                            )
                        } else {
                            stringResource(R.string.series_empty)
                        }
                    )
                }
            }
        ) { series ->
            PosterCard(
                title = series.name,
                posterUrl = series.posterUrl,
                onClick = { onOpenSeries(series.id) },
                onLongClick = { onEvent(SeriesEvent.ToggleFavorite(series.id)) },
                ratingBadge = series.rating?.badge,
                isFavorite = series.id in browser.favoriteIds,
                progress = browser.progressById[series.id]?.fraction
            )
        }
    }
}
