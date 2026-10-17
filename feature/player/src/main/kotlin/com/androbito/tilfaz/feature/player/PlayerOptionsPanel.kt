package com.androbito.tilfaz.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** The pages of the options panel: a root list that leads to one page per setting. */
sealed interface OptionsPage {
    data object Root : OptionsPage
    data object Audio : OptionsPage
    data object Subtitles : OptionsPage
}

/**
 * The player's right-hand options panel. Back steps from a page to the root, and from the root closes the panel.
 * A null subtitle selection means "Off".
 */
@Composable
fun PlayerOptionsPanel(
    page: OptionsPage,
    onPage: (OptionsPage) -> Unit,
    onClose: () -> Unit,
    audio: List<TrackOption>,
    subtitles: List<TrackOption>,
    onSelectAudio: (TrackOption) -> Unit,
    onSelectSubtitle: (TrackOption?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { if (page == OptionsPage.Root) onClose() else onPage(OptionsPage.Root) }

    val off = stringResource(R.string.player_options_off)
    val noneFound = stringResource(R.string.player_options_none_found)
    val currentAudio = audio.firstOrNull { it.isSelected }?.label ?: noneFound
    val currentSubtitle = subtitles.firstOrNull { it.isSelected }?.label ?: off

    Column(
        modifier.background(TilfazColors.Surface).padding(Dimens.SpaceL).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Text(stringResource(R.string.player_options_title), style = MaterialTheme.typography.titleLarge, color = TilfazColors.OnSurfaceMuted)
        when (page) {
            OptionsPage.Root -> OptionRows(
                rows = listOf(
                    OptionRow(stringResource(R.string.player_options_audio), currentAudio, selected = false) { onPage(OptionsPage.Audio) },
                    OptionRow(stringResource(R.string.player_options_subtitles), currentSubtitle, selected = false) { onPage(OptionsPage.Subtitles) }
                ),
                pageKey = page
            )
            OptionsPage.Audio -> OptionRows(
                rows = audio.map { OptionRow(it.label, it.detail, it.isSelected) { onSelectAudio(it) } },
                pageKey = page
            )
            OptionsPage.Subtitles -> OptionRows(
                rows = listOf(OptionRow(off, null, selected = subtitles.none { it.isSelected }) { onSelectSubtitle(null) }) +
                    subtitles.map { OptionRow(it.label, it.detail, it.isSelected) { onSelectSubtitle(it) } },
                pageKey = page
            )
        }
    }
}

private class OptionRow(val title: String, val detail: String?, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun OptionRows(rows: List<OptionRow>, pageKey: OptionsPage) {
    val initialFocus = remember(pageKey) { FocusRequester() }
    val startIndex = rows.indexOfFirst { it.selected }.coerceAtLeast(0)
    LaunchedEffect(pageKey) {
        withFrameNanos { } // the rows join the focus tree once their first frame is laid out
        runCatching { initialFocus.requestFocus() }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        items(rows.size) { index ->
            val row = rows[index]
            FocusableSurface(
                onClick = row.onClick,
                modifier = Modifier.fillMaxWidth().then(if (index == startIndex) Modifier.focusRequester(initialFocus) else Modifier),
                corner = Dimens.CornerS,
                focusedScale = 1.0f
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(Dimens.SpaceM),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        row.detail?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TilfazColors.OnSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (row.selected) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
