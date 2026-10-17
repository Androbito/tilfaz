package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/** The remote's Menu ("options") key while this item has focus: the same job as a long press on OK, one key away. */
fun Modifier.onOptionKey(onOption: () -> Unit): Modifier = onKeyEvent { event ->
    if (event.key == Key.Menu) {
        if (event.type == KeyEventType.KeyUp) onOption()
        true
    } else {
        false
    }
}
