package com.blockchain.componentlib.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.NoRippleProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    buttonContent: @Composable RowScope.(state: ButtonState, text: String, textColor: Color, textAlpha: Float, icon: ImageResource) -> Unit,
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
    pressedBackgroundTimeShown: Long = 250L,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    icon: ImageResource = ImageResource.None,
) {

    val interactionSource = remember { MutableInteractionSource() }

    var backgroundColor by remember(isDarkTheme) {
        mutableStateOf(if (isDarkTheme) defaultBackgroundDarkColor else defaultBackgroundLightColor)
    }

    val disabledBackgroundColor = if (isDarkTheme) disabledBackgroundDarkColor else disabledBackgroundLightColor

    val textAlpha = when (state) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) disabledTextDarkAlpha else disabledTextLightAlpha
        ButtonState.Loading -> 0f
    }

    LaunchedEffect(interactionSource, state, isDarkTheme) {

        fun cancel() {
            backgroundColor = if (isDarkTheme) {
                defaultBackgroundDarkColor
            } else {
                defaultBackgroundLightColor
            }
        }

        interactionSource.interactions.collectPressInteractions(
            onPressed = {
                if (state == ButtonState.Enabled) {
                    backgroundColor = pressedBackgroundColor
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
        androidx.compose.material.Button(
            onClick = { onClick.takeIf { state == ButtonState.Enabled }?.invoke() },
            modifier = modifier.requiredHeightIn(min = 48.dp),
            enabled = state != ButtonState.Disabled,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = animateColorAsState(targetValue = backgroundColor).value,
                contentColor = Color.Unspecified,
                disabledBackgroundColor = animateColorAsState(targetValue = disabledBackgroundColor).value,
                disabledContentColor = Color.Unspecified,
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            content = {
                buttonContent(
                    state = state,
                    textColor = defaultTextColor,
                    text = text,
                    textAlpha = textAlpha,
                    icon = icon,
                )
            }
        )
    }
}

public suspend inline fun <T> Flow<T>.collectPressInteractions(
    crossinline onPressed: suspend () -> Unit,
    crossinline onRelease: suspend () -> Unit,
    crossinline onCancel: suspend () -> Unit
) {
    collect { interaction ->
        when (interaction) {
            is PressInteraction -> {
                when (interaction) {
                    is PressInteraction.Press -> onPressed()
                    is PressInteraction.Release -> onRelease()
                    is PressInteraction.Cancel -> onCancel()
                }
            }
        }
    }
}
