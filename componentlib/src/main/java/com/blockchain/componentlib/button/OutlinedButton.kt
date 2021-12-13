package com.blockchain.componentlib.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.image.ImageResource
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
import com.blockchain.componentlib.theme.NoRippleProvider
import kotlinx.coroutines.delay

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
        icon: ImageResource,
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
    pressedBackgroundTimeShown: Long = 250L,
    shape: Shape = AppTheme.shapes.small,
    state: ButtonState = ButtonState.Enabled,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    icon: ImageResource = ImageResource.None,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    var backgroundColor by remember { mutableStateOf(Color.Unspecified) }
    var borderColor by remember {
        mutableStateOf(
            if (isDarkTheme) defaultBorderDarkColor else defaultBorderLightColor
        )
    }

    val textColor = when (state) {
        ButtonState.Enabled -> AppTheme.colors.primary
        ButtonState.Disabled -> {
            if (isDarkTheme) disabledTextDarkColor else disabledTextLightColor
        }
        ButtonState.Loading -> Color.Unspecified
    }

    val textAlpha = when (state) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) disabledTextDarkAlpha else disabledTextLightAlpha
        ButtonState.Loading -> 0f
    }

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, state, isDarkTheme) {

        fun cancel() {
            backgroundColor = Color.Unspecified
            borderColor = when (state) {
                ButtonState.Disabled -> {
                    if (isDarkTheme) disabledBorderDarkColor else disabledBorderLightColor
                }
                else -> {
                    if (isDarkTheme) defaultBorderDarkColor else defaultBorderLightColor
                }
            }
        }

        interactionSource.interactions.collectPressInteractions(
            onPressed = {
                if (state == ButtonState.Enabled) {
                    backgroundColor = if (isDarkTheme) pressedButtonDarkColor else pressedButtonLightColor
                    borderColor = if (isDarkTheme) pressedBorderDarkColor else pressedBorderLightColor
                }
            },
            onRelease = {
                delay(pressedBackgroundTimeShown)
                cancel()
            },
            onCancel = {
                cancel()
            },
        )
    }

    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleProvider
    ) {
        OutlinedButton(
            onClick = { onClick.takeIf { state == ButtonState.Enabled }?.invoke() },
            modifier = modifier,
            enabled = state != ButtonState.Disabled,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = animateColorAsState(targetValue = backgroundColor).value,
                contentColor = Color.Unspecified,
                disabledContentColor = Color.Unspecified,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = animateColorAsState(targetValue = borderColor).value
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            contentPadding = contentPadding,
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
                    },
                    icon = icon,
                )
            }
        )
    }
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
                    icon: ImageResource,
                    ->
                    ButtonContentSmall(
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
