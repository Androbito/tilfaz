package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import androidx.compose.foundation.BorderStroke

/**
 * The one focus treatment used everywhere: slight scale, cyan border and glow when focused.
 * Keeping it in a single composable is what makes the app feel coherent on a D-pad.
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    corner: Dp = Dimens.CornerM,
    containerColor: Color = TilfazColors.Surface,
    focusedContainerColor: Color = TilfazColors.SurfaceBright,
    selectedContainerColor: Color = TilfazColors.Accent,
    focusedScale: Float = 1.04f,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) selectedContainerColor else containerColor,
            focusedContainerColor = if (selected) selectedContainerColor else focusedContainerColor,
            pressedContainerColor = focusedContainerColor,
            contentColor = TilfazColors.OnBackground,
            focusedContentColor = TilfazColors.OnBackground,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TilfazColors.OnBackground), shape = shape),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = TilfazColors.Accent, elevation = 12.dp),
        ),
        content = content,
    )
}
