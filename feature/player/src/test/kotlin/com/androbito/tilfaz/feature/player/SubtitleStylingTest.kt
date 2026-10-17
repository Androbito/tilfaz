package com.androbito.tilfaz.feature.player

import androidx.media3.ui.CaptionStyleCompat
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleStyle
import org.junit.Test

class SubtitleStylingTest {
    @Test
    fun `the chosen text colour and background reach the caption style`() {
        val style = SubtitleStyle(textColor = SubtitleColor.YELLOW, background = SubtitleBackground.BLUE).toCaptionStyle()

        assertThat(style.foregroundColor).isEqualTo(SubtitleColor.YELLOW.argb)
        assertThat(style.backgroundColor).isEqualTo(SubtitleBackground.BLUE.argb)
    }

    @Test
    fun `text without a background gets an outline so it stays readable`() {
        val style = SubtitleStyle(background = SubtitleBackground.NONE).toCaptionStyle()

        assertThat(style.edgeType).isEqualTo(CaptionStyleCompat.EDGE_TYPE_OUTLINE)
    }

    @Test
    fun `text on a background needs no outline`() {
        val style = SubtitleStyle(background = SubtitleBackground.TRANSLUCENT).toCaptionStyle()

        assertThat(style.edgeType).isEqualTo(CaptionStyleCompat.EDGE_TYPE_NONE)
    }
}
