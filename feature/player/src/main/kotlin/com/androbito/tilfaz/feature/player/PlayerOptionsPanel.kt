package com.androbito.tilfaz.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.component.backKeyAsBackPress
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import com.androbito.tilfaz.core.model.SubtitleStyle

/** The pages of the options panel: a root list that leads to one page per setting. */
sealed interface OptionsPage {
    data object Root : OptionsPage
    data object Audio : OptionsPage
    data object Subtitles : OptionsPage
    data object Style : OptionsPage
    data object Speed : OptionsPage
    data object Aspect : OptionsPage
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
    style: SubtitleStyle,
    onStyle: (SubtitleStyle) -> Unit,
    speed: Float,
    onSpeed: (Float) -> Unit,
    aspect: AspectMode,
    onAspect: (AspectMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { if (page == OptionsPage.Root) onClose() else onPage(OptionsPage.Root) }

    val off = stringResource(R.string.player_options_off)
    val noneFound = stringResource(R.string.player_options_none_found)
    val currentAudio = audio.firstOrNull { it.isSelected }?.label ?: noneFound
    val currentSubtitle = subtitles.firstOrNull { it.isSelected }?.label ?: off

    Column(
        modifier
            .background(TilfazColors.Surface)
            .padding(Dimens.SpaceL)
            .focusGroup()
            .backKeyAsBackPress(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Text(stringResource(R.string.player_options_title), style = MaterialTheme.typography.titleLarge, color = TilfazColors.OnSurfaceMuted)
        when (page) {
            OptionsPage.Root -> OptionRows(
                rows = listOf(
                    OptionRow(stringResource(R.string.player_options_audio), currentAudio, selected = false) { onPage(OptionsPage.Audio) },
                    OptionRow(stringResource(R.string.player_options_subtitles), currentSubtitle, selected = false) { onPage(OptionsPage.Subtitles) },
                    OptionRow(stringResource(R.string.player_options_subtitle_style), null, selected = false) { onPage(OptionsPage.Style) },
                    OptionRow(stringResource(R.string.player_options_speed), speedLabel(speed), selected = false) { onPage(OptionsPage.Speed) },
                    OptionRow(stringResource(R.string.player_options_aspect), stringResource(aspect.label), selected = false) { onPage(OptionsPage.Aspect) }
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
            OptionsPage.Style -> OptionRows(
                rows = listOf(
                    OptionRow(stringResource(R.string.player_options_style_size), stringResource(style.size.label), selected = false) {
                        onStyle(style.copy(size = style.size.next(SubtitleSize.entries)))
                    },
                    OptionRow(stringResource(R.string.player_options_style_color), stringResource(style.textColor.label), selected = false) {
                        onStyle(style.copy(textColor = style.textColor.next(SubtitleColor.entries)))
                    },
                    OptionRow(stringResource(R.string.player_options_style_background), stringResource(style.background.label), selected = false) {
                        onStyle(style.copy(background = style.background.next(SubtitleBackground.entries)))
                    }
                ),
                pageKey = page,
                header = { StylePreview(style) }
            )
            OptionsPage.Speed -> OptionRows(
                rows = SPEEDS.map { OptionRow(speedLabel(it), null, selected = it == speed) { onSpeed(it) } },
                pageKey = page
            )
            OptionsPage.Aspect -> OptionRows(
                rows = AspectMode.entries.map { OptionRow(stringResource(it.label), null, selected = it == aspect) { onAspect(it) } },
                pageKey = page
            )
        }
    }
}

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun speedLabel(speed: Float) = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

private fun <T : Enum<T>> T.next(entries: List<T>): T = entries[(ordinal + 1) % entries.size]

/** How the choices will look on screen, so the user does not have to close the panel to judge them. */
@Composable
private fun StylePreview(style: SubtitleStyle) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.CornerS)).background(Color.DarkGray).padding(Dimens.SpaceL),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.player_options_style_sample),
            color = Color(style.textColor.argb),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize =
                PREVIEW_BASE_SP.sp * style.size.fractionOfVideoHeight / SubtitleSize.MEDIUM.fractionOfVideoHeight
            ),
            modifier = Modifier.background(Color(style.background.argb)).padding(horizontal = Dimens.SpaceS)
        )
    }
}

private const val PREVIEW_BASE_SP = 20f

private class OptionRow(val title: String, val detail: String?, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun OptionRows(rows: List<OptionRow>, pageKey: OptionsPage, header: (@Composable () -> Unit)? = null) {
    val initialFocus = remember(pageKey) { FocusRequester() }
    val startIndex = rows.indexOfFirst { it.selected }.coerceAtLeast(0)
    LaunchedEffect(pageKey) {
        withFrameNanos { } // the rows join the focus tree once their first frame is laid out
        runCatching { initialFocus.requestFocus() }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        header?.let { item { it() } }
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
