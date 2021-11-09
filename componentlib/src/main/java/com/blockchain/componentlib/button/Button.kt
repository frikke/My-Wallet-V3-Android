package com.blockchain.componentlib.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    buttonContent: @Composable RowScope.(state: ButtonState, text: String, textColor: Color, textAlpha: Float) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.small,
    defaultTextColor: Color = Color.Unspecified,
    defaultBackgroundLightColor: Color = Color.Unspecified,
    defaultBackgroundDarkColor: Color = Color.Unspecified,
    disabledTextLightAlpha: Float = 0.7f,
    disabledTextDarkAlpha: Float = 0.4f,
    disabledBackgroundLightColor: Color = Color.Unspecified,
    disabledBackgroundDarkColor: Color = Color.Unspecified,
    pressedBackgroundColor: Color = Color.Unspecified,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val backgroundColor =
        if (isPressed && state == ButtonState.Enabled) {
            pressedBackgroundColor
        } else {
            if (isDarkTheme) defaultBackgroundDarkColor else defaultBackgroundLightColor
        }

    val textAlpha = when (state) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) disabledTextDarkAlpha else disabledTextLightAlpha
        ButtonState.Loading -> 0f
    }

    androidx.compose.material.Button(
        onClick = { onClick.takeIf { state == ButtonState.Enabled }?.invoke() },
        modifier = modifier.requiredHeightIn(min = 48.dp),
        enabled = state != ButtonState.Disabled,
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = Color.Unspecified,
            disabledBackgroundColor = if (isDarkTheme) disabledBackgroundDarkColor else disabledBackgroundLightColor,
            disabledContentColor = Color.Unspecified,
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        content = { buttonContent(state = state, textColor = defaultTextColor, text = text, textAlpha = textAlpha) }
    )
}
