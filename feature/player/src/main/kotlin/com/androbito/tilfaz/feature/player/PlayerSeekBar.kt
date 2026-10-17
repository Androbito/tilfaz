package com.androbito.tilfaz.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.formatPlayerTime
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/**
 * The bar under a film. While [scrub] is set the cursor leaves the playhead and a bubble above it names the time it
 * points at and how far that is from where playback is; nothing is sought until the scrub is committed.
 */
@Composable
fun PlayerSeekBar(positionMs: Long, durationMs: Long, scrub: ScrubState?, modifier: Modifier = Modifier) {
    fun fractionOf(ms: Long) = if (durationMs <= 0) 0f else (ms.toFloat() / durationMs).coerceIn(0f, 1f)
    val played = fractionOf(positionMs)
    val cursor = fractionOf(scrub?.targetMs ?: positionMs)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        // The bubble's row is always reserved so the bar does not jump when a scrub starts.
        Box(Modifier.fillMaxWidth().height(BubbleRowHeight)) {
            if (scrub != null) {
                Row(
                    Modifier
                        .align(BiasAlignment(horizontalBias = cursor * 2 - 1, verticalBias = 0f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceXs),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
                ) {
                    Text(formatPlayerTime(scrub.targetMs), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(formatScrubDelta(scrub.deltaMs), style = MaterialTheme.typography.titleMedium, color = TilfazColors.Accent)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(ThumbSize), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(TrackHeight).clip(RoundedCornerShape(TrackHeight / 2)).background(Color.White.copy(alpha = 0.25f)))
            Box(Modifier.fillMaxWidth(played).height(TrackHeight).clip(RoundedCornerShape(TrackHeight / 2)).background(TilfazColors.Accent))
            Box(
                Modifier
                    .align(BiasAlignment(horizontalBias = cursor * 2 - 1, verticalBias = 0f))
                    .size(if (scrub != null) ThumbSize else ThumbSize * 2 / 3)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPlayerTime(positionMs), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(formatPlayerTime(durationMs), style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

private val BubbleRowHeight = 40.dp
private val TrackHeight = 6.dp
private val ThumbSize = 20.dp
