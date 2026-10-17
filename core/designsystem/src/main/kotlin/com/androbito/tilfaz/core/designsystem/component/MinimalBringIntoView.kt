package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import kotlin.math.abs

/**
 * Scrolls a focused item into view only when it is not already fully visible, and clear of a [marginPx] border so a
 * focused button never ends up flush against the edge of a TV (inside the overscan zone).
 *
 * On Leanback devices Compose's default parks every focused item at 30% of the viewport, so a detail page opens already
 * scrolled whenever its first button sits lower than that line (a two-line Arabic title is enough), cutting off the title.
 */
@OptIn(ExperimentalFoundationApi::class)
class MinimalBringIntoViewSpec(private val marginPx: Float = 0f) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val windowStart = marginPx
        val windowEnd = containerSize - marginPx
        val trailingEdge = offset + size
        return when {
            offset >= windowStart && trailingEdge <= windowEnd -> 0f
            offset < windowStart && trailingEdge > windowEnd -> 0f
            abs(offset - windowStart) < abs(trailingEdge - windowEnd) -> offset - windowStart
            else -> trailingEdge - windowEnd
        }
    }
}

/** Wrap a vertically scrolling page so focus moves scroll it minimally instead of pivoting. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideMinimalBringIntoView(content: @Composable () -> Unit) {
    val margin = with(LocalDensity.current) { Dimens.SpaceL.toPx() }
    val spec = remember(margin) { MinimalBringIntoViewSpec(margin) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
