package com.blockchain.componentlib.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Dark300
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey700

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonContent: @Composable (
        state: ButtonState,
        text: String,
        textColor: Color,
        textAlpha: Float,
        loadingIconResId: Int,
    ) -> Unit,
    pressedButtonLightColor: Color = Grey000,
    pressedButtonDarkColor: Color = Dark800,
    pressedBorderLightColor: Color = Blue600,
    pressedBorderDarkColor: Color = Blue400,
    disabledTextLightAlpha: Float = 0.7f,
    disabledTextDarkAlpha: Float = 1f,
    disabledTextLightColor: Color = Blue600,
    disabledTextDarkColor: Color = Grey600,
    disabledBorderLightColor: Color = Grey000,
    disabledBorderDarkColor: Color = Grey700,
    defaultBorderLightColor: Color = Grey100,
    defaultBorderDarkColor: Color = Dark300,
    shape: Shape = AppTheme.shapes.small,
    state: ButtonState = ButtonState.Enabled,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val backgroundColor =
        if (isPressed && state == ButtonState.Enabled) {
            if (isDarkTheme) pressedButtonDarkColor else pressedButtonLightColor
        } else {
            Color.Unspecified
        }

    val textAlpha = when (state) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) disabledTextDarkAlpha else disabledTextLightAlpha
        ButtonState.Loading -> 0f
    }

    val textColor = when (state) {
        ButtonState.Enabled -> AppTheme.colors.primary
        ButtonState.Disabled -> {
            if (isDarkTheme) disabledTextDarkColor else disabledTextLightColor
        }
        ButtonState.Loading -> Color.Unspecified
    }

    val borderColor = when (state) {
        ButtonState.Enabled -> {
            if (isPressed) {
                if (isDarkTheme) pressedBorderDarkColor else pressedBorderLightColor
            } else {
                if (isDarkTheme) defaultBorderDarkColor else defaultBorderLightColor
            }
        }
        ButtonState.Disabled -> {
            if (isDarkTheme) disabledBorderDarkColor else disabledBorderLightColor
        }
        ButtonState.Loading -> {
            if (isDarkTheme) defaultBorderDarkColor else defaultBorderLightColor
        }
    }

    OutlinedButton(
        onClick = { onClick.takeIf { state == ButtonState.Enabled }?.invoke() },
        modifier = modifier.requiredHeightIn(min = 48.dp),
        enabled = state != ButtonState.Disabled,
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor,
            contentColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified,
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        content = {
            buttonContent(
                state = state,
                textColor = textColor,
                text = text,
                textAlpha = textAlpha,
                loadingIconResId = if (isDarkTheme) {
                    R.drawable.ic_loading_minimal_dark
                } else {
                    R.drawable.ic_loading_minimal_light
                }
            )
        }
    )
}

@Preview
@Composable
private fun OutlineButtonPreview() {
    AppTheme {
        AppSurface {
            OutlinedButton(
                text = "Click me",
                onClick = { },
                buttonContent = {
                    state: ButtonState,
                    text: String,
                    textColor: Color,
                    textAlpha: Float,
                    loadingIconResId: Int,
                    ->

                    ResizableButtonContent(
                        state = state,
                        text = text,
                        textColor = textColor,
                        textAlpha = textAlpha,
                        loadingIconResId = loadingIconResId
                    )
                },
            )
        }
    }
}
