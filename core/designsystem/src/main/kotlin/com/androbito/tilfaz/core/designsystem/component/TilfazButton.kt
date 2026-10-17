package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** Primary action: blue container and a white label in every state; a disabled button is the only dim one. */
@Composable
fun TilfazButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = tilfazButtonColors(),
        border = tilfazButtonBorder(),
        content = content
    )
}

@Composable
private fun tilfazButtonColors(): ButtonColors = ButtonDefaults.colors(
    containerColor = TilfazColors.AccentSoft,
    contentColor = TilfazColors.OnAccent,
    focusedContainerColor = TilfazColors.AccentSoft,
    focusedContentColor = TilfazColors.OnAccent,
    pressedContainerColor = TilfazColors.AccentSoft,
    pressedContentColor = TilfazColors.OnAccent,
    disabledContainerColor = TilfazColors.SurfaceVariant,
    disabledContentColor = TilfazColors.OnSurfaceMuted
)

/** The container keeps its colour on focus, so a white ring (plus the stock focus scale) shows the selection. */
@Composable
private fun tilfazButtonBorder(): ButtonBorder = ButtonDefaults.border(
    focusedBorder = Border(BorderStroke(3.dp, TilfazColors.OnAccent))
)
