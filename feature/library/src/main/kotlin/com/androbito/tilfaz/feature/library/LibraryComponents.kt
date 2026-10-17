package com.androbito.tilfaz.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.ListFocusMemory
import com.androbito.tilfaz.core.designsystem.component.ProvideMinimalBringIntoView
import com.androbito.tilfaz.core.designsystem.component.TilfazButton
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** The question shown over a library page before a change is made, e.g. "Remove from favourites?". */
data class Confirmation(val title: String, val message: String, val confirmLabel: String, val cancelLabel: String)

/** What focus must do once the question is answered. */
private sealed interface FocusAfterQuestion {
    /** Nothing changed: the item is still there. */
    data object Kept : FocusAfterQuestion

    /** The item leaves its list, so wait for the item count to change before picking the neighbour that slides in. */
    data class Changed(val itemCountBefore: Int) : FocusAfterQuestion
}

/**
 * A page made of horizontal shelves (see [shelf]) with a confirmation question that can open over it. Items register
 * with [focusMemory] so that Back from the player lands on the item that was played, and so that focus finds a sensible
 * place when the question closes.
 */
@Composable
fun LibraryPage(
    title: String,
    hint: String,
    itemCount: Int,
    focusMemory: ListFocusMemory,
    confirmation: Confirmation?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    var focusAfterQuestion by remember { mutableStateOf<FocusAfterQuestion?>(null) }
    val confirm = {
        focusAfterQuestion = FocusAfterQuestion.Changed(itemCount)
        onConfirm()
    }
    val cancel = {
        focusAfterQuestion = FocusAfterQuestion.Kept
        onCancel()
    }
    LaunchedEffect(focusAfterQuestion, itemCount, confirmation) {
        val pending = focusAfterQuestion ?: return@LaunchedEffect
        if (confirmation != null) return@LaunchedEffect
        val ready = when (pending) {
            FocusAfterQuestion.Kept -> true
            is FocusAfterQuestion.Changed -> itemCount != pending.itemCountBefore
        }
        if (ready) {
            focusMemory.restore(itemCount)
            focusAfterQuestion = null
        }
    }
    BackHandler(enabled = confirmation != null) { cancel() }

    ProvideMinimalBringIntoView {
        Box(Modifier.fillMaxSize()) {
            // While the question is open the page behind it must not take focus.
            Column(Modifier.fillMaxSize().focusProperties { canFocus = confirmation == null }) {
                TopBar(title = "Tilfaz · $title")
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceS),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
                    modifier = Modifier.weight(1f)
                ) {
                    content()
                    if (itemCount > 0) {
                        item { Text(hint, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted) }
                    }
                }
            }
            if (confirmation != null) ConfirmationDialog(confirmation, onConfirm = confirm, onCancel = cancel)
        }
    }
}

/** One titled shelf: a heading, then either [row] or [emptyHint] when there is nothing to show. */
fun LazyListScope.shelf(title: String, emptyHint: String, isEmpty: Boolean, row: @Composable () -> Unit) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
            if (isEmpty) {
                Text(emptyHint, style = MaterialTheme.typography.bodyLarge, color = TilfazColors.OnSurfaceMuted)
            } else {
                row()
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(confirmation: Confirmation, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { } // the button joins the focus tree once its first frame is laid out
        cancelFocus.requestFocus()
    }
    Box(Modifier.fillMaxSize().background(TilfazColors.Scrim), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(520.dp).background(TilfazColors.Surface, RoundedCornerShape(Dimens.CornerL)).padding(Dimens.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            Text(confirmation.title, style = MaterialTheme.typography.headlineSmall)
            Text(confirmation.message, style = MaterialTheme.typography.bodyLarge, color = TilfazColors.OnSurfaceMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM), modifier = Modifier.padding(top = Dimens.SpaceS)) {
                // The safe choice is the one focus starts on.
                OutlinedButton(onClick = onCancel, modifier = Modifier.focusRequester(cancelFocus)) { Text(confirmation.cancelLabel) }
                TilfazButton(onClick = onConfirm) { Text(confirmation.confirmLabel) }
            }
        }
    }
}
