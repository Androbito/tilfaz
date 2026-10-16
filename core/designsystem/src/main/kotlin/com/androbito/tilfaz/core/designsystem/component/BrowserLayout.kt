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

/**
 * The two-pane browser used by Movies and Series: a search box and a section list on the left,
 * a poster grid on the right. Features supply the data and the item composable.
 */
@Composable
fun <T> BrowserLayout(
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    sections: LazyListScope.() -> Unit,
    items: List<T>,
    itemKey: (T) -> Any,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
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
                sections()
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (items.isEmpty()) {
                emptyContent()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Dimens.PosterWidth),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    contentPadding = PaddingValues(start = Dimens.SpaceS, end = Dimens.SpaceS, top = Dimens.SpaceS, bottom = Dimens.SpaceXl)
                ) {
                    items(items, key = itemKey) { itemContent(it) }
                }
            }
        }
    }
}
