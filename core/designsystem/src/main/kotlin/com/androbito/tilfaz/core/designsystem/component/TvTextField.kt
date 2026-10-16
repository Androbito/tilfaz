package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

/** TV Material has no text field; this is a focus-aware BasicTextField styled to match the tiles. */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    errorText: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        errorText != null -> TilfazColors.Danger
        focused -> TilfazColors.Accent
        else -> TilfazColors.SurfaceVariant
    }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TilfazColors.OnSurfaceMuted, modifier = Modifier.padding(bottom = Dimens.SpaceXs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(TilfazColors.Surface, RoundedCornerShape(Dimens.CornerS))
                .border(2.dp, borderColor, RoundedCornerShape(Dimens.CornerS))
                .padding(horizontal = Dimens.SpaceM),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TilfazColors.OnSurfaceMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interaction,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TilfazColors.OnBackground),
                cursorBrush = SolidColor(TilfazColors.Accent),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }, onGo = { onImeAction() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (errorText != null) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = TilfazColors.Danger, modifier = Modifier.padding(top = Dimens.SpaceXs))
        }
    }
}
