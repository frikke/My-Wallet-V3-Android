package com.blockchain.componentlib.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier,
    defaultTextColor: Color = Color.Unspecified,
    defaultBackgroundColor: Color = Color.Unspecified,
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
            defaultBackgroundColor
        }

    val textAlpha = when (state) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) disabledTextDarkAlpha else disabledTextLightAlpha
        ButtonState.Loading -> 0f
    }

    androidx.compose.material.Button(
        onClick = { onClick.takeIf { state == ButtonState.Enabled }?.invoke() },
        modifier = modifier.height(48.dp),
        enabled = state != ButtonState.Disabled,
        interactionSource = interactionSource,
        shape = AppTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = Color.Unspecified,
            disabledBackgroundColor = if (isDarkTheme) disabledBackgroundDarkColor else disabledBackgroundLightColor,
            disabledContentColor = Color.Unspecified,
        ),
        content = {
            Box {
                if (state == ButtonState.Loading) {
                    ButtonLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
                Text(
                    text = text,
                    color = defaultTextColor,
                    modifier = Modifier.alpha(textAlpha)
                )
            }
        }
    )
}
