package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/**
 * Big gradient tile for Live TV / Movies / Series on the home screen, with a "last updated" footer and refresh.
 *
 * The tile and its refresh button are two separate focus targets side by side, not one inside the other: a button nested in a
 * clickable tile can never take the remote's focus. Down moves from the tile to its refresh button, Up moves back.
 */
@Composable
fun HeroTile(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    lastUpdatedLabel: String,
    isSyncing: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    /** Applied to the big clickable area only, e.g. a focus requester so other items can send focus to it. */
    bodyModifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimens.CornerL)
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hasFocus) FOCUSED_SCALE else 1f, label = "tileScale")
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { hasFocus = it.hasFocus }
            .clip(shape)
            .background(gradient)
            .border(if (hasFocus) 3.dp else 0.dp, if (hasFocus) TilfazColors.OnBackground else Color.Transparent, shape)
    ) {
        // The whole tile draws the focus ring and scale, so this area adds none of its own.
        Surface(
            onClick = onClick,
            modifier = bodyModifier.weight(1f).fillMaxWidth().semantics { contentDescription = title },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = ClickableSurfaceDefaults.border(),
            glow = ClickableSurfaceDefaults.glow()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
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

private const val FOCUSED_SCALE = 1.03f

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
