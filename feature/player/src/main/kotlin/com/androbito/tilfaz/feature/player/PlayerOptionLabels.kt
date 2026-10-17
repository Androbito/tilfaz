package com.androbito.tilfaz.feature.player

import androidx.annotation.StringRes
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize

/** How the video fills the screen. */
enum class AspectMode(@StringRes val label: Int) {
    FIT(R.string.player_aspect_fit),
    FILL(R.string.player_aspect_fill),
    STRETCH(R.string.player_aspect_stretch)
}

@get:StringRes
val SubtitleSize.label: Int
    get() = when (this) {
        SubtitleSize.SMALL -> R.string.player_size_small
        SubtitleSize.MEDIUM -> R.string.player_size_medium
        SubtitleSize.LARGE -> R.string.player_size_large
        SubtitleSize.EXTRA_LARGE -> R.string.player_size_extra_large
    }

@get:StringRes
val SubtitleColor.label: Int
    get() = when (this) {
        SubtitleColor.WHITE -> R.string.player_color_white
        SubtitleColor.YELLOW -> R.string.player_color_yellow
        SubtitleColor.CYAN -> R.string.player_color_cyan
        SubtitleColor.GREEN -> R.string.player_color_green
    }

@get:StringRes
val SubtitleBackground.label: Int
    get() = when (this) {
        SubtitleBackground.NONE -> R.string.player_background_none
        SubtitleBackground.TRANSLUCENT -> R.string.player_background_translucent
        SubtitleBackground.BLACK -> R.string.player_background_black
        SubtitleBackground.GRAY -> R.string.player_background_gray
        SubtitleBackground.BLUE -> R.string.player_background_blue
    }
