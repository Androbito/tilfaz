package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androbito.tilfaz.core.designsystem.theme.Dimens

private const val POSTERS_PER_ROW = 4

/**
 * The two-pane browser used by Movies and Series: a search box and a section list on the left
 * (suggestions instead while a query is typed), a poster grid on the right. Features supply the data and the item composable.
 */
@Composable
fun <T : Any> BrowserLayout(
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    sections: LazyListScope.() -> Unit,
    /** The grid is given by count and lookup, so a paged catalog is never copied into a list. [itemAt] is null for one not loaded yet. */
    itemCount: () -> Int,
    itemAt: (Int) -> T?,
    itemKey: (Int) -> Any,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    /** Shown in place of [sections] while a query is typed. */
    suggestions: (LazyListScope.() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val focusMemory = rememberListFocusMemory(initialIndex = 0)
    Row(modifier.fillMaxSize().padding(horizontal = Dimens.SpaceL), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
        Column(Modifier.width(Dimens.SidebarWidth).fillMaxHeight()) {
            TvTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "",
                placeholder = searchPlaceholder,
                modifier = Modifier.padding(bottom = Dimens.SpaceS)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = Dimens.SpaceXl)) {
                if (suggestions != null && query.isNotBlank()) suggestions() else sections()
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (itemCount() == 0) {
                emptyContent()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(POSTERS_PER_ROW),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    contentPadding = PaddingValues(start = Dimens.SpaceM, end = Dimens.SpaceM, top = Dimens.SpaceM, bottom = Dimens.SpaceXl)
                ) {
                    // Read here, in the grid's own content, so the count and the keys are always from the same version of a paged list.
                    items(count = itemCount(), key = { index -> if (index < itemCount()) itemKey(index) else "past-the-end-$index" }) { index ->
                        val item = itemAt(index)
                        // propagateMinConstraints keeps the grid cell's width tight, as if the item were placed directly.
                        Box(Modifier.rememberFocus(focusMemory, index), propagateMinConstraints = true) { if (item != null) itemContent(item) }
                    }
                }
            }
        }
    }
}
