package com.androbito.tilfaz.core.designsystem.component

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * The TV spends the first Back on dropping the focus of the item that holds it, and only the second one goes back.
 * Back is taken here, before that happens, and replayed as a normal back press for whichever handler is active.
 */
@Composable
fun Modifier.backKeyAsBackPress(): Modifier {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return onPreviewKeyEvent { event ->
        if (event.key != Key.Back || dispatcher == null) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyUp) dispatcher.onBackPressed()
        true
    }
}
