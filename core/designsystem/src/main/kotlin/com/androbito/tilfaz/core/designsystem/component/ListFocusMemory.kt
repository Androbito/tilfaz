package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Which item of a list last had focus. It lives in saveable state, so it outlives the screen being disposed
 * when a detail page opens, and Back lands on the item the user left instead of the top of the screen.
 */
@Stable
class ListFocusMemory internal constructor(initialIndex: Int) {
    var index by mutableIntStateOf(initialIndex)
        internal set

    internal val requester = FocusRequester()

    /** True when focus must be given back: the memory came from saved state, or the screen asked for a starting item. */
    internal var restorePending = initialIndex >= 0

    /**
     * Gives focus back to the remembered item now, e.g. after a question closed over the list. If the list got shorter
     * the last item takes over; an empty list has nothing to focus.
     */
    suspend fun restore(itemCount: Int) {
        if (itemCount <= 0) return
        index = index.coerceIn(0, itemCount - 1)
        withFrameNanos { } // the item at the (possibly new) index picks up the requester
        requester.requestFocus()
    }

    internal companion object {
        val Saver = Saver<ListFocusMemory, Int>(save = { it.index }, restore = { ListFocusMemory(it) })
    }
}

/** [initialIndex] is the item to focus on a fresh start; the default -1 leaves the first start to the system. */
@Composable
fun rememberListFocusMemory(initialIndex: Int = -1): ListFocusMemory =
    rememberSaveable(saver = ListFocusMemory.Saver) { ListFocusMemory(initialIndex) }

/** Put this on one list item: it records focus, and takes focus back once if the screen is being restored. */
fun Modifier.rememberFocus(memory: ListFocusMemory, index: Int): Modifier = this
    .then(
        if (index == memory.index) {
            Modifier.focusRequester(memory.requester).onGloballyPositioned {
                if (memory.restorePending) {
                    memory.restorePending = false
                    memory.requester.requestFocus()
                }
            }
        } else {
            Modifier
        }
    )
    .onFocusChanged { if (it.hasFocus) memory.index = index }
