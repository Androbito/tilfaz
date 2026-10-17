package com.androbito.tilfaz.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.HeroTile
import com.androbito.tilfaz.core.designsystem.component.PillTile
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.lastUpdatedLabel
import com.androbito.tilfaz.core.designsystem.component.rememberCurrentTime
import com.androbito.tilfaz.core.designsystem.component.rememberFocus
import com.androbito.tilfaz.core.designsystem.component.rememberListFocusMemory
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

class HomeNavigation(
    val onOpenLive: () -> Unit,
    val onOpenMovies: () -> Unit,
    val onOpenSeries: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenFavorites: () -> Unit,
    val onOpenContinueWatching: () -> Unit
)

fun NavGraphBuilder.homeScreen(navigation: HomeNavigation) {
    composable<HomeRoute> { HomeRoute(navigation) }
}

@Composable
fun HomeRoute(navigation: HomeNavigation, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onEvent = viewModel::onEvent, navigation = navigation)
}

@Composable
fun HomeScreen(state: HomeUiState, onEvent: (HomeEvent) -> Unit, navigation: HomeNavigation) {
    val now by rememberCurrentTime()
    // Tiles are numbered in reading order; the LIVE tile (0) takes focus on a fresh start.
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    Column(Modifier.fillMaxSize()) {
        TopBar {
            IconButton(onClick = navigation.onOpenSearch) { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.home_search)) }
            IconButton(onClick = navigation.onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings)) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = Dimens.SpaceM)) {
                Text(state.accountName, style = MaterialTheme.typography.titleMedium)
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.home_account_label),
                    tint = TilfazColors.Accent,
                    modifier = Modifier.padding(start = Dimens.SpaceS).size(28.dp)
                )
            }
        }

        Row(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            HeroTile(
                title = stringResource(R.string.home_live_tv),
                icon = Icons.Default.LiveTv,
                gradient = TilfazColors.LiveGradient,
                lastUpdatedLabel = lastUpdatedLabel(state.status(ContentType.LIVE).lastSyncedAt, now),
                isSyncing = state.status(ContentType.LIVE).isSyncing,
                onClick = navigation.onOpenLive,
                onRefresh = { onEvent(HomeEvent.Refresh(ContentType.LIVE)) },
                modifier = Modifier.weight(1f).fillMaxSize().rememberFocus(focusMemory, 0)
            )
            HeroTile(
                title = stringResource(R.string.home_movies),
                icon = Icons.Default.Movie,
                gradient = TilfazColors.MoviesGradient,
                lastUpdatedLabel = lastUpdatedLabel(state.status(ContentType.VOD).lastSyncedAt, now),
                isSyncing = state.status(ContentType.VOD).isSyncing,
                onClick = navigation.onOpenMovies,
                onRefresh = { onEvent(HomeEvent.Refresh(ContentType.VOD)) },
                modifier = Modifier.weight(1f).fillMaxSize().rememberFocus(focusMemory, 1)
            )
            HeroTile(
                title = stringResource(R.string.home_series),
                icon = Icons.Default.Theaters,
                gradient = TilfazColors.SeriesGradient,
                lastUpdatedLabel = lastUpdatedLabel(state.status(ContentType.SERIES).lastSyncedAt, now),
                isSyncing = state.status(ContentType.SERIES).isSyncing,
                onClick = navigation.onOpenSeries,
                onRefresh = { onEvent(HomeEvent.Refresh(ContentType.SERIES)) },
                modifier = Modifier.weight(1f).fillMaxSize().rememberFocus(focusMemory, 2)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL).height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            PillTile(
                stringResource(R.string.home_favorites),
                Icons.Default.Favorite,
                navigation.onOpenFavorites,
                Modifier.weight(1f).rememberFocus(focusMemory, 3)
            )
            PillTile(
                stringResource(R.string.home_continue),
                Icons.Default.History,
                navigation.onOpenContinueWatching,
                Modifier.weight(1f).rememberFocus(focusMemory, 4)
            )
            PillTile(
                stringResource(R.string.home_settings),
                Icons.Default.Settings,
                navigation.onOpenSettings,
                Modifier.weight(1f).rememberFocus(focusMemory, 5)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.SpaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(expirationLabel(state), style = MaterialTheme.typography.titleMedium, color = TilfazColors.OnSurfaceMuted)
            SyncErrors(state.syncErrors)
        }
    }
}

@Composable
private fun expirationLabel(state: HomeUiState): String {
    val status = state.accountStatus ?: return ""
    val expires = status.expiresAt ?: return stringResource(R.string.home_expiration_unlimited)
    val formatted = expires.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    val base = stringResource(R.string.home_expiration, formatted)
    return if (status.isTrial) "$base · ${stringResource(R.string.home_trial)}" else base
}

@Composable
private fun SyncErrors(errors: Map<ContentType, DomainError>) {
    if (errors.isEmpty()) return
    Column(horizontalAlignment = Alignment.End) {
        errors.forEach { (type, error) ->
            val typeLabel = when (type) {
                ContentType.LIVE -> stringResource(R.string.home_live_tv)
                ContentType.VOD -> stringResource(R.string.home_movies)
                ContentType.SERIES -> stringResource(R.string.home_series)
            }
            val reason = when (error) {
                DomainError.Network -> stringResource(R.string.home_error_network)
                DomainError.Unauthorized, DomainError.NotSignedIn -> stringResource(R.string.home_error_unauthorized)
                DomainError.AccountExpired -> stringResource(R.string.home_error_expired)
                is DomainError.Server, is DomainError.BadResponse -> stringResource(R.string.home_error_server)
                is DomainError.Unknown -> stringResource(R.string.home_error_unknown)
            }
            Text(stringResource(R.string.home_sync_failed, typeLabel, reason), style = MaterialTheme.typography.bodyMedium, color = TilfazColors.Danger)
        }
    }
}
