package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.abs

/**
 * Scrolls a focused item into view only when it is not already fully visible.
 *
 * On Leanback devices Compose's default parks every focused item at 30% of the viewport, so a detail page opens already
 * scrolled whenever its first button sits lower than that line (a two-line Arabic title is enough), cutting off the title.
 */
@OptIn(ExperimentalFoundationApi::class)
object MinimalBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        return when {
            offset >= 0f && trailingEdge <= containerSize -> 0f
            offset < 0f && trailingEdge > containerSize -> 0f
            abs(offset) < abs(trailingEdge - containerSize) -> offset
            else -> trailingEdge - containerSize
        }
    }
}

/** Wrap a vertically scrolling page so focus moves scroll it minimally instead of pivoting. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideMinimalBringIntoView(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides MinimalBringIntoViewSpec, content = content)
}
