package com.androbito.tilfaz.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class LiveRoute(val initialCategoryId: String? = null) {
    companion object {
        const val ARG_INITIAL_CATEGORY = "initialCategoryId"
    }
}

/**
 * The small live video of the right-hand pane. It is provided by the player feature (features never depend on each other)
 * and receives the channel to show, the space to fill, and what to call when the user presses OK on it.
 */
typealias LivePreviewSlot = @Composable (channel: LiveChannel?, handingOff: Boolean, modifier: Modifier, onOpen: () -> Unit) -> Unit

fun NavGraphBuilder.liveScreen(onPlayChannel: (channelId: Int, categoryId: String?) -> Unit, preview: LivePreviewSlot) {
    composable<LiveRoute> { LiveRoute(onPlayChannel, preview) }
}

@Composable
fun LiveRoute(onPlayChannel: (Int, String?) -> Unit, preview: LivePreviewSlot, viewModel: LiveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channels = viewModel.channels.collectAsLazyPagingItems()
    LiveScreen(
        state = state,
        channels = channels,
        onEvent = viewModel::onEvent,
        preview = preview,
        onPlay = { channel ->
            viewModel.onEvent(LiveEvent.Play(channel.id))
            onPlayChannel(channel.id, zapCategoryFor(state, channel))
        }
    )
}

/**
 * The category the player pages through with channel up and down. A channel found by search may belong to a different
 * category than the one selected, and paging has to happen among its own neighbours.
 */
internal fun zapCategoryFor(state: LiveUiState, channel: LiveChannel): String? =
    if (state.query.isNotBlank()) {
        channel.categoryId
    } else {
        state.selectedCategory?.takeIf { it.kind == LiveCategoryUi.Kind.PROVIDER }?.id
    }

@Composable
fun LiveScreen(
    state: LiveUiState,
    channels: LazyPagingItems<LiveChannel>,
    onEvent: (LiveEvent) -> Unit,
    onPlay: (LiveChannel) -> Unit,
    preview: LivePreviewSlot = { _, _, _, _ -> }
) {
    val now by rememberCurrentTime()
    // The account allows one connection: once a channel is opened full screen, the small video goes away at once so that
    // the two never stream together. It is allowed back when this screen is shown again.
    // Saved with the screen: when Back brings it again it is rebuilt still suspended, so it waits out the pause once more and
    // does not start a stream that the settling focus would cut a moment later.
    var previewSuspended by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            delay(LiveViewModel.PREVIEW_DELAY_MS)
            previewSuspended = false
        }
    }
    val play: (LiveChannel) -> Unit = { channel ->
        previewSuspended = true
        onPlay(channel)
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tilfaz · ${stringResource(R.string.live_title)}")
        Row(Modifier.fillMaxSize().padding(horizontal = Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            CategoryColumn(state, onEvent, play, Modifier.width(SidebarWidth).fillMaxHeight())
            ChannelColumn(state, channels, onEvent, play, now, Modifier.weight(1f).fillMaxHeight())
            PreviewPane(
                channel = state.focusedChannel,
                nowNext = state.focusedChannelId?.let { state.nowNext[it] },
                now = now,
                isFavorite = state.focusedChannelId in state.favoriteIds,
                video = state.previewChannel.takeUnless { previewSuspended },
                handingOff = previewSuspended,
                onOpenVideo = play,
                preview = preview,
                modifier = Modifier.width(PreviewWidth).fillMaxHeight()
            )
        }
    }
}

// The three columns share 960dp on a 1080p TV; the channel list gets what these two leave, enough for logo, name and programme.
private val SidebarWidth = 270.dp
private val PreviewWidth = 300.dp
private const val VIDEO_ASPECT = 16f / 9f

@Composable
private fun CategoryColumn(state: LiveUiState, onEvent: (LiveEvent) -> Unit, onPlay: (LiveChannel) -> Unit, modifier: Modifier) {
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
                    SuggestionRow(TitleText.clean(channel.name), onClick = { onPlay(channel) })
                }
                return@LazyColumn
            }
            items(state.categories, key = { it.id }) { category ->
                val (name, icon) = when (category.kind) {
                    LiveCategoryUi.Kind.ALL -> stringResource(R.string.live_all) to Icons.Default.ViewList
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelColumn(
    state: LiveUiState,
    channels: LazyPagingItems<LiveChannel>,
    onEvent: (LiveEvent) -> Unit,
    onPlay: (LiveChannel) -> Unit,
    now: Instant,
    modifier: Modifier
) {
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    val firstChannelFocus = remember { FocusRequester() }
    // A new category or search is a new list: it starts at its first channel, not at the scroll position of the previous one.
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedCategoryId, state.query) { listState.scrollToItem(0) }
    Box(modifier) {
        when {
            state.isLoading -> LoadingState()
            // While the first page loads the list is empty too, and that is not "no results".
            channels.itemCount == 0 -> if (channels.loadState.refresh !is LoadState.Loading) {
                EmptyState(
                    title = when {
                        state.query.isNotBlank() -> stringResource(R.string.live_no_results, state.query)
                        state.selectedCategoryId == LiveCategoryUi.HISTORY_ID -> stringResource(R.string.live_empty_history)
                        else -> stringResource(R.string.live_empty_category)
                    }
                )
            }
            else -> LazyColumn(
                state = listState,
                // From the categories, Right lands on the first channel; by position it would be the one level with the category.
                modifier = Modifier.focusProperties {
                    enter =
                        { direction -> if (direction == FocusDirection.Right) firstChannelFocus else FocusRequester.Default }
                }.focusGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = Dimens.SpaceXl)
            ) {
                // The count is read inside the list's own content so it and the keys always come from the same version of the paged list.
                items(count = channels.itemCount, key = { index ->
                    if (index <
                        channels.itemCount
                    ) {
                        channels.itemKey { it.id }(index)
                    } else {
                        "past-the-end-$index"
                    }
                }) { index ->
                    val channel = channels[index] ?: return@items
                    val nowNext = state.nowNext[channel.id]
                    ChannelRow(
                        name = channel.name,
                        logoUrl = channel.logoUrl,
                        nowTitle = nowNext?.now?.title,
                        progress = nowNext?.now?.progressAt(now),
                        selected = channel.id == state.focusedChannelId,
                        onClick = { onPlay(channel) },
                        onLongClick = { onEvent(LiveEvent.ToggleFavorite(channel.id)) },
                        isFavorite = channel.id in state.favoriteIds,
                        modifier = Modifier.rememberFocus(focusMemory, index)
                            .then(if (index == 0) Modifier.focusRequester(firstChannelFocus) else Modifier)
                            .onFocusChanged { if (it.isFocused) onEvent(LiveEvent.FocusChannel(channel)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(
    channel: LiveChannel?,
    nowNext: NowNext?,
    now: Instant,
    isFavorite: Boolean,
    video: LiveChannel?,
    handingOff: Boolean,
    onOpenVideo: (LiveChannel) -> Unit,
    preview: LivePreviewSlot,
    modifier: Modifier
) {
    Column(modifier.padding(start = Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        if (channel == null) return
        // The small live video, or the channel's logo while nothing is playing (scrolling, or just opened).
        Box(Modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT).background(TilfazColors.Surface, RoundedCornerShape(Dimens.CornerS))) {
            // Always composed, so the player behind it survives scrolling; with no channel it only shows the logo.
            preview(video, handingOff, Modifier.fillMaxSize()) { onOpenVideo(channel) }
            if (video == null) ChannelLogo(channel.logoUrl, Modifier.size(72.dp).align(Alignment.Center))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel.logoUrl, Modifier.size(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.hasArchive && channel.archiveDays > 0) {
                    Text(
                        pluralStringResource(R.plurals.live_archive_days, channel.archiveDays, channel.archiveDays),
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
                nowNext.now?.let { ProgrammeBlock(stringResource(R.string.live_now), it, now, descriptionLines = 2) }
                nowNext.next?.let { ProgrammeBlock(stringResource(R.string.live_next), it, null, descriptionLines = 0) }
            }
        }
        if (isFavorite) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = TilfazColors.Danger, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.live_in_favorites), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            stringResource(R.string.live_press_ok),
            style = MaterialTheme.typography.bodyMedium,
            color = TilfazColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = if (descriptionLines > 0) 2 else 1, overflow = TextOverflow.Ellipsis)
        if (now != null) ProgressBar(entry.progressAt(now), Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceXs).height(4.dp))
        if (descriptionLines > 0) {
            entry.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted, maxLines = descriptionLines) }
        }
    }
}
