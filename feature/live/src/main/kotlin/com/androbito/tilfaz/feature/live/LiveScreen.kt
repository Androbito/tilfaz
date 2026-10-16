package com.androbito.tilfaz.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.CategoryRow
import com.androbito.tilfaz.core.designsystem.component.ChannelLogo
import com.androbito.tilfaz.core.designsystem.component.ChannelRow
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.ProgressBar
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.component.rememberCurrentTime
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class LiveRoute(val initialCategoryId: String? = null) {
    companion object {
        const val ARG_INITIAL_CATEGORY = "initialCategoryId"
        const val FAVORITES = LiveCategoryUi.FAVORITES_ID
    }
}

fun NavGraphBuilder.liveScreen(onPlayChannel: (channelId: Int, categoryId: String?) -> Unit) {
    composable<LiveRoute> { LiveRoute(onPlayChannel) }
}

@Composable
fun LiveRoute(onPlayChannel: (Int, String?) -> Unit, viewModel: LiveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LiveScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onPlay = { id ->
            viewModel.onEvent(LiveEvent.Play(id))
            val providerCategory = state.selectedCategory?.takeIf { it.kind == LiveCategoryUi.Kind.PROVIDER }?.id
            onPlayChannel(id, providerCategory)
        },
    )
}

@Composable
fun LiveScreen(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (Int) -> Unit) {
    val now by rememberCurrentTime()
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.live_title)}")
        Row(Modifier.fillMaxSize().padding(horizontal = Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
            CategoryColumn(state, onEvent, Modifier.width(Dimens.SidebarWidth).fillMaxHeight())
            ChannelColumn(state, onEvent, onPlay, now, Modifier.weight(1f).fillMaxHeight())
            PreviewPane(state.focusedChannel, state.focusedChannelId?.let { state.nowNext[it] }, now, Modifier.width(420.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun CategoryColumn(state: LiveUiState, onEvent: (LiveEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        TvTextField(
            value = state.query,
            onValueChange = { onEvent(LiveEvent.QueryChanged(it)) },
            label = "",
            placeholder = stringResource(R.string.live_search_categories),
            modifier = Modifier.padding(bottom = Dimens.SpaceS),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
            items(state.categories, key = { it.id }) { category ->
                val (name, icon) = when (category.kind) {
                    LiveCategoryUi.Kind.ALL -> stringResource(R.string.live_all) to Icons.Default.ViewList
                    LiveCategoryUi.Kind.FAVORITES -> stringResource(R.string.live_favorites) to Icons.Default.Favorite
                    LiveCategoryUi.Kind.HISTORY -> stringResource(R.string.live_history) to Icons.Default.History
                    LiveCategoryUi.Kind.PROVIDER -> category.name to null
                }
                CategoryRow(
                    name = name,
                    count = category.count,
                    selected = category.id == state.selectedCategoryId,
                    onClick = { onEvent(LiveEvent.SelectCategory(category.id)) },
                    icon = icon,
                )
            }
        }
    }
}

@Composable
private fun ChannelColumn(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (Int) -> Unit, now: Instant, modifier: Modifier) {
    Box(modifier) {
        when {
            state.isLoading -> LoadingState()
            state.channels.isEmpty() -> EmptyState(
                title = when {
                    state.query.isNotBlank() -> stringResource(R.string.live_no_results, state.query)
                    state.selectedCategoryId == LiveCategoryUi.FAVORITES_ID -> stringResource(R.string.live_empty_favorites)
                    state.selectedCategoryId == LiveCategoryUi.HISTORY_ID -> stringResource(R.string.live_empty_history)
                    else -> stringResource(R.string.live_empty_category)
                },
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
                items(state.channels, key = { it.id }) { channel ->
                    val nowNext = state.nowNext[channel.id]
                    ChannelRow(
                        name = channel.name,
                        logoUrl = channel.logoUrl,
                        nowTitle = nowNext?.now?.title,
                        progress = nowNext?.now?.progressAt(now),
                        selected = channel.id == state.focusedChannelId,
                        onClick = { onPlay(channel.id) },
                        onLongClick = { onEvent(LiveEvent.ToggleFavorite(channel.id)) },
                        isFavorite = channel.id in state.favoriteIds,
                        modifier = Modifier.onFocusChanged { if (it.isFocused) onEvent(LiveEvent.FocusChannel(channel.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(channel: LiveChannel?, nowNext: NowNext?, now: Instant, modifier: Modifier) {
    Column(modifier.padding(start = Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        if (channel == null) return
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            ChannelLogo(channel.logoUrl, Modifier.size(96.dp))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(channel.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                if (channel.hasArchive && channel.archiveDays > 0) {
                    Text(stringResource(R.string.live_archive_days, channel.archiveDays), style = MaterialTheme.typography.bodyMedium, color = TilfazColors.Accent)
                }
            }
        }
        when {
            nowNext == null -> Text(stringResource(R.string.live_epg_unavailable), color = TilfazColors.OnSurfaceMuted)
            nowNext.now == null && nowNext.next == null -> Text(stringResource(R.string.live_epg_unavailable), color = TilfazColors.OnSurfaceMuted)
            else -> {
                nowNext.now?.let { ProgrammeBlock(stringResource(R.string.live_now), it, now) }
                nowNext.next?.let { ProgrammeBlock(stringResource(R.string.live_next), it, null) }
            }
        }
        Text(stringResource(R.string.live_press_ok), style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted)
    }
}

@Composable
private fun ProgrammeBlock(label: String, entry: EpgEntry, now: Instant?) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text("$label · ${entry.start.atZone(zone).format(fmt)} - ${entry.end.atZone(zone).format(fmt)}", style = MaterialTheme.typography.labelLarge, color = TilfazColors.Accent)
        Text(entry.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
        if (now != null) ProgressBar(entry.progressAt(now), Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceXs).height(4.dp))
        entry.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted, maxLines = 6) }
    }
}
