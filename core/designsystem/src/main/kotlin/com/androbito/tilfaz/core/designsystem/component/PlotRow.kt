package com.androbito.tilfaz.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.theme.Dimens

/** A synopsis cut at [collapsedLines] lines: OK reads all of it, OK or Back folds it again. */
@Composable
fun PlotRow(label: String, text: String?, collapsedLines: Int, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    FocusableSurface(
        onClick = { expanded = !expanded },
        modifier = modifier
            .fillMaxWidth()
            .then(if (expanded) Modifier.backKeyAsBackPress() else Modifier)
            .semantics {
                if (expanded) {
                    collapse { expanded = false; true }
                } else {
                    expand { expanded = true; true }
                }
            },
        corner = Dimens.CornerS,
        focusedScale = 1f
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.SpaceS), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(LabelWidth))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Same column as the other info rows of the details pages, so the plot lines up with them.
private val LabelWidth = 180.dp
