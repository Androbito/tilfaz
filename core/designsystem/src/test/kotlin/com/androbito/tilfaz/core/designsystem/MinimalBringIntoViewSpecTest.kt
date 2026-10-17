package com.androbito.tilfaz.core.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.designsystem.component.MinimalBringIntoViewSpec
import org.junit.Test

/** Distances are what the scroll container must move by; offset is the item's leading edge relative to the viewport. */
@OptIn(ExperimentalFoundationApi::class)
class MinimalBringIntoViewSpecTest {
    private val container = 900f

    private fun distance(offset: Float, size: Float) = MinimalBringIntoViewSpec.calculateScrollDistance(offset, size, container)

    @Test
    fun `an item that is already fully visible does not scroll the page`() {
        assertThat(distance(offset = 300f, size = 100f)).isEqualTo(0f)
        assertThat(distance(offset = 0f, size = 100f)).isEqualTo(0f)
        assertThat(distance(offset = 800f, size = 100f)).isEqualTo(0f)
    }

    @Test
    fun `an item below the viewport scrolls just enough to show its bottom edge`() {
        assertThat(distance(offset = 850f, size = 100f)).isEqualTo(50f)
    }

    @Test
    fun `an item above the viewport scrolls just enough to show its top edge`() {
        assertThat(distance(offset = -40f, size = 100f)).isEqualTo(-40f)
    }

    @Test
    fun `an item taller than the viewport that already covers it is left alone`() {
        assertThat(distance(offset = -20f, size = 1000f)).isEqualTo(0f)
    }
}
