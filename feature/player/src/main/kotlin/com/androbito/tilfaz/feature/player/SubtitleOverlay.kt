package com.androbito.tilfaz.feature.player

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleStyle

/** Outline only when there is no background box: light text on a bright scene is unreadable without one or the other. */
@UnstableApi
fun SubtitleStyle.toCaptionStyle(): CaptionStyleCompat = CaptionStyleCompat(
    textColor.argb,
    background.argb,
    Color.TRANSPARENT,
    if (background == SubtitleBackground.NONE) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
    Color.BLACK,
    null
)

/** Draws the current cues over the video with the user's style; embedded styling is ignored so the choice always wins. */
@OptIn(UnstableApi::class)
@Composable
fun SubtitleOverlay(cues: List<Cue>, style: SubtitleStyle, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            SubtitleView(context).apply { setApplyEmbeddedStyles(false) }
        },
        update = { view ->
            view.setStyle(style.toCaptionStyle())
            view.setFractionalTextSize(style.size.fractionOfVideoHeight)
            view.setCues(cues)
        },
        modifier = modifier
    )
}
