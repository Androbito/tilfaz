package com.androbito.tilfaz.core.designsystem.component

import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Back belongs to the back dispatcher (BackHandler, Navigation) and to nothing else. Left alone, Compose also reads Back as
 * "move focus out of the focused item": inside a focus group, such as a lazy row, that succeeds, drops the focus and eats the
 * press, so it took two presses to go back.
 *
 * Put this once, at the root. It is an [onKeyEvent], which bubbles up from the focused item: whatever below wanted the key
 * (a text field, a panel) has already had it, and only an unwanted Back ends up here, where it is taken so that focus stays.
 * From Android 13 the system calls the dispatcher's callback by itself. Before that Back arrives only as this key, so it is
 * handed to the dispatcher here, once, on release.
 */
@Composable
fun Modifier.backKeyForDispatcherOnly(): Modifier {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val systemCallsBack = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return onKeyEvent { event ->
        if (event.key != Key.Back) return@onKeyEvent false
        if (!systemCallsBack && event.type == KeyEventType.KeyUp) dispatcher?.onBackPressed()
        true
    }
}
