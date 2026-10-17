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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.CategoryRow
import com.androbito.tilfaz.core.designsystem.component.ChannelLogo
import com.androbito.tilfaz.core.designsystem.component.ChannelRow
import com.androbito.tilfaz.core.designsystem.component.EmptyState
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.ProgressBar
import com.androbito.tilfaz.core.designsystem.component.SuggestionRow
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.component.rememberCurrentTime
import com.androbito.tilfaz.core.designsystem.component.rememberFocus
import com.androbito.tilfaz.core.designsystem.component.rememberListFocusMemory
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.model.TitleText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable
data class LiveRoute(val initialCategoryId: String? = null) {
    companion object {
        const val ARG_INITIAL_CATEGORY = "initialCategoryId"
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
            onPlayChannel(id, zapCategoryFor(state, id))
        }
    )
}

/**
 * The category the player pages through with channel up and down. A channel found by search may belong to a different
 * category than the one selected, and paging has to happen among its own neighbours.
 */
internal fun zapCategoryFor(state: LiveUiState, channelId: Int): String? =
    if (state.query.isNotBlank()) {
        state.channels.firstOrNull { it.id == channelId }?.categoryId
    } else {
        state.selectedCategory?.takeIf { it.kind == LiveCategoryUi.Kind.PROVIDER }?.id
    }

@Composable
fun LiveScreen(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (Int) -> Unit) {
    val now by rememberCurrentTime()
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.live_title)}")
        Row(Modifier.fillMaxSize().padding(horizontal = Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            CategoryColumn(state, onEvent, onPlay, Modifier.width(SidebarWidth).fillMaxHeight())
            ChannelColumn(state, onEvent, onPlay, now, Modifier.weight(1f).fillMaxHeight())
            PreviewPane(
                channel = state.focusedChannel,
                nowNext = state.focusedChannelId?.let { state.nowNext[it] },
                now = now,
                isFavorite = state.focusedChannelId in state.favoriteIds,
                modifier = Modifier.width(PreviewWidth).fillMaxHeight()
            )
        }
    }
}

// The three columns share 960dp on a 1080p TV; the channel list gets what these two leave, enough for logo, name and programme.
private val SidebarWidth = 270.dp
private val PreviewWidth = 300.dp

@Composable
private fun CategoryColumn(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (Int) -> Unit, modifier: Modifier) {
    Column(modifier) {
        TvTextField(
            value = state.query,
            onValueChange = { onEvent(LiveEvent.QueryChanged(it)) },
            label = "",
            placeholder = stringResource(R.string.live_search_categories),
            modifier = Modifier.padding(bottom = Dimens.SpaceS)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
            if (state.query.isNotBlank()) {
                items(state.suggestions, key = { it.id }) { channel ->
                    SuggestionRow(TitleText.clean(channel.name), onClick = { onPlay(channel.id) })
                }
                return@LazyColumn
            }
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
                    icon = icon
                )
            }
        }
    }
}

@Composable
private fun ChannelColumn(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (Int) -> Unit, now: Instant, modifier: Modifier) {
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    Box(modifier) {
        when {
            state.isLoading -> LoadingState()
            state.channels.isEmpty() -> EmptyState(
                title = when {
                    state.query.isNotBlank() -> stringResource(R.string.live_no_results, state.query)
                    state.selectedCategoryId == LiveCategoryUi.FAVORITES_ID -> stringResource(R.string.live_empty_favorites)
                    state.selectedCategoryId == LiveCategoryUi.HISTORY_ID -> stringResource(R.string.live_empty_history)
                    else -> stringResource(R.string.live_empty_category)
                }
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
                itemsIndexed(state.channels, key = { _, channel -> channel.id }) { index, channel ->
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
                        modifier = Modifier.rememberFocus(focusMemory, index).onFocusChanged { if (it.isFocused) onEvent(LiveEvent.FocusChannel(channel.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(channel: LiveChannel?, nowNext: NowNext?, now: Instant, isFavorite: Boolean, modifier: Modifier) {
    Column(modifier.padding(start = Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        if (channel == null) return
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            ChannelLogo(channel.logoUrl, Modifier.size(72.dp))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(channel.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                if (channel.hasArchive && channel.archiveDays > 0) {
                    Text(
                        stringResource(R.string.live_archive_days, channel.archiveDays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TilfazColors.Accent
                    )
                }
            }
        }
        when {
            nowNext == null -> Text(stringResource(R.string.live_epg_unavailable), color = TilfazColors.OnSurfaceMuted)
            nowNext.now == null && nowNext.next == null -> Text(stringResource(R.string.live_epg_unavailable), color = TilfazColors.OnSurfaceMuted)
            else -> {
                nowNext.now?.let { ProgrammeBlock(stringResource(R.string.live_now), it, now, descriptionLines = 3) }
                nowNext.next?.let { ProgrammeBlock(stringResource(R.string.live_next), it, null, descriptionLines = 0) }
            }
        }
        if (isFavorite) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = TilfazColors.Danger, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.live_in_favorites), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.live_press_ok), style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted)
    }
}

@Composable
private fun ProgrammeBlock(label: String, entry: EpgEntry, now: Instant?, descriptionLines: Int) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            "$label · ${entry.start.atZone(zone).format(fmt)} - ${entry.end.atZone(zone).format(fmt)}",
            style = MaterialTheme.typography.labelLarge,
            color = TilfazColors.Accent
        )
        Text(entry.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
        if (now != null) ProgressBar(entry.progressAt(now), Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceXs).height(4.dp))
        if (descriptionLines > 0) {
            entry.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted, maxLines = descriptionLines) }
        }
    }
}
