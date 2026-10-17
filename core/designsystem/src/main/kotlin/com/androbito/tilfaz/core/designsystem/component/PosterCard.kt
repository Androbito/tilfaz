package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/**
 * Portrait poster with the rating badge in the corner and the title overlaid at the bottom,
 * like the movie/series grids the user knows. [progress] draws a resume bar when non-null.
 */
@Composable
fun PosterCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ratingBadge: String? = null,
    isFavorite: Boolean = false,
    progress: Float? = null,
    onLongClick: (() -> Unit)? = null
) {
    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(Dimens.PosterWidth).aspectRatio(Dimens.PosterWidth / Dimens.PosterHeight),
        corner = Dimens.CornerS,
        focusedScale = 1.08f
    ) {
        Box(Modifier.fillMaxSize()) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(TilfazColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = TilfazColors.OnSurfaceMuted, modifier = Modifier.size(48.dp))
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, TilfazColors.Scrim)))
                    .padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceS)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (ratingBadge != null) {
                RatingBadge(ratingBadge, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceS))
            }
            if (isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.ds_favorite),
                    tint = TilfazColors.Danger,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceS).size(22.dp)
                )
            }
            if (progress != null && progress > 0f) {
                ProgressBar(progress, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp))
            }
        }
    }
}

@Composable
fun RatingBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TilfazColors.Badge)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = TilfazColors.Accent) {
    Box(modifier.background(Color.White.copy(alpha = 0.25f))) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(color)
        )
    }
}
