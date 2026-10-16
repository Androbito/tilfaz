package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** Big gradient tile for Live TV / Movies / Series on the home screen, with a "last updated" footer and refresh. */
@Composable
fun HeroTile(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    lastUpdatedLabel: String,
    isSyncing: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = title },
        corner = Dimens.CornerL,
        containerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        focusedScale = 1.03f
    ) {
        Column(Modifier.fillMaxSize().background(gradient)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.padding(top = Dimens.SpaceM)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSyncing) stringResource(R.string.ds_loading) else lastUpdatedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ds_refresh), tint = Color.White)
                }
            }
        }
    }
}

/** Smaller gradient pill used for the secondary row (favourites, continue watching, settings). */
@Composable
fun PillTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = TilfazColors.PillGradient
) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier,
        corner = Dimens.CornerM,
        containerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(gradient).padding(horizontal = Dimens.SpaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
