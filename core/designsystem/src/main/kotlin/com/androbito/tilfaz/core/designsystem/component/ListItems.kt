package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** Left-column category entry: name on the left, item count on the right, cyan when selected. */
@Composable
fun CategoryRow(
    name: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FocusableSurface(
        onClick = onClick,
        selected = selected,
        modifier = modifier.fillMaxWidth().height(60.dp),
        corner = Dimens.CornerS,
        containerColor = Color.Transparent,
        focusedScale = 1.0f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceM).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (count != null) {
                Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, color = if (selected) TilfazColors.OnBackground else TilfazColors.OnSurfaceMuted)
            }
        }
    }
}

/** Channel entry: logo, name, now-playing title with a thin progress bar. */
@Composable
fun ChannelRow(
    name: String,
    logoUrl: String?,
    nowTitle: String?,
    progress: Float?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
) {
    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        modifier = modifier.fillMaxWidth().height(84.dp),
        corner = Dimens.CornerS,
        containerColor = Color.Transparent,
        focusedScale = 1.0f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceM).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            ChannelLogo(logoUrl, Modifier.size(Dimens.ChannelLogo))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nowTitle ?: stringResource(R.string.ds_no_program),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) TilfazColors.OnBackground else TilfazColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress != null) {
                    ProgressBar(progress, Modifier.fillMaxWidth().height(3.dp).padding(top = 2.dp), color = if (selected) Color.White else TilfazColors.Accent)
                }
            }
            if (isFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.ds_favorite), tint = TilfazColors.Danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChannelLogo(logoUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(Dimens.CornerS)).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null) {
            AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(4.dp).size(Dimens.ChannelLogo))
        } else {
            Icon(Icons.Default.Tv, contentDescription = null, tint = TilfazColors.OnSurfaceMuted)
        }
    }
}
