package com.blockchain.componentlib.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.divider.VerticalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Blue700
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.NoRippleProvider
import kotlinx.coroutines.delay

@Composable
fun DoublePrimaryButtons(
    startButtonText: String,
    onStartButtonClick: () -> Unit,
    endButtonText: String,
    onEndButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedBackgroundTimeShown: Long = 250L,
    startButtonState: ButtonState = ButtonState.Enabled,
    endButtonState: ButtonState = ButtonState.Enabled,
    startButtonIcon: ImageResource = ImageResource.None,
    endButtonIcon: ImageResource = ImageResource.None,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val startButtonInteractionSource = remember { MutableInteractionSource() }
    var startButtonBackgroundColor by remember {
        mutableStateOf(
            when (startButtonState) {
                ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
                else -> Blue600
            }
        )
    }

    val endButtonInteractionSource = remember { MutableInteractionSource() }
    var endButtonBackgroundColor by remember {
        mutableStateOf(
            when (startButtonState) {
                ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
                else -> Blue600
            }
        )
    }

    var dividerAlpha by remember { mutableStateOf(0.4f) }

    val startTextAlpha = when (startButtonState) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) 0.4f else 0.7f
        ButtonState.Loading -> 0f
    }
    val endTextAlpha = when (startButtonState) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) 0.4f else 0.7f
        ButtonState.Loading -> 0f
    }

    LaunchedEffect(startButtonInteractionSource, startButtonState, isDarkTheme) {
        fun cancel() {
            startButtonBackgroundColor = when (startButtonState) {
                ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
                else -> Blue600
            }
            dividerAlpha = 0.4f
        }

        startButtonInteractionSource.interactions.collectPressInteractions(
            onPressed = {
                if (startButtonState == ButtonState.Enabled) {
                    startButtonBackgroundColor = Blue700
                    dividerAlpha = 0f
                }
            },
            onRelease = {
                delay(pressedBackgroundTimeShown)
                cancel()
            },
            onCancel = {
                cancel()
            }
        )
    }

    LaunchedEffect(endButtonInteractionSource, endButtonState, isDarkTheme) {
        fun cancel() {
            endButtonBackgroundColor = when (startButtonState) {
                ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
                else -> Blue600
            }
            dividerAlpha = 0.4f
        }

        endButtonInteractionSource.interactions.collectPressInteractions(
            onPressed = {
                if (endButtonState == ButtonState.Enabled) {
                    endButtonBackgroundColor = Blue700
                    dividerAlpha = 0f
                }
            },
            onRelease = {
                delay(pressedBackgroundTimeShown)
                cancel()
            },
            onCancel = {
                cancel()
            }
        )
    }

    CompositionLocalProvider(
        LocalRippleTheme provides NoRippleProvider
    ) {
        Row(
            modifier = modifier
                .requiredHeightIn(min = 48.dp)
                .height(IntrinsicSize.Max)
        ) {
            androidx.compose.material.Button(
                onClick = { onStartButtonClick.takeIf { startButtonState == ButtonState.Enabled }?.invoke() },
                modifier = modifier
                    .fillMaxHeight()
                    .weight(1f),
                enabled = startButtonState != ButtonState.Disabled,
                interactionSource = startButtonInteractionSource,
                shape = AppTheme.shapes.small.copy(
                    topEnd = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                ),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = animateColorAsState(targetValue = startButtonBackgroundColor).value,
                    contentColor = Color.Unspecified,
                    disabledBackgroundColor = animateColorAsState(targetValue = startButtonBackgroundColor).value,
                    disabledContentColor = Color.Unspecified
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                content = {
                    ButtonContent(
                        state = startButtonState,
                        text = startButtonText,
                        textColor = Color.White,
                        contentAlpha = startTextAlpha,
                        icon = startButtonIcon
                    )
                }
            )

            VerticalDivider(
                dividerColor = Color.White,
                modifier = Modifier
                    .background(animateColorAsState(targetValue = startButtonBackgroundColor).value)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
                    .alpha(animateFloatAsState(targetValue = dividerAlpha).value)
            )

            androidx.compose.material.Button(
                onClick = { onEndButtonClick.takeIf { endButtonState == ButtonState.Enabled }?.invoke() },
                modifier = modifier
                    .fillMaxHeight()
                    .weight(1f),
                enabled = endButtonState != ButtonState.Disabled,
                interactionSource = endButtonInteractionSource,
                shape = AppTheme.shapes.small.copy(
                    topStart = CornerSize(0.dp),
                    bottomStart = CornerSize(0.dp)
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = animateColorAsState(targetValue = endButtonBackgroundColor).value,
                    contentColor = Color.Unspecified,
                    disabledBackgroundColor = animateColorAsState(targetValue = endButtonBackgroundColor).value,
                    disabledContentColor = Color.Unspecified
                ),
                content = {
                    ButtonContent(
                        state = endButtonState,
                        text = endButtonText,
                        textColor = Color.White,
                        contentAlpha = endTextAlpha,
                        icon = endButtonIcon
                    )
                }
            )
        }
    }
}

@Preview(name = "Default", group = "Double primary buttons", device = Devices.PIXEL, fontScale = 2.7f)
@Composable
private fun DoublePrimary_Buttons() {
    AppTheme {
        AppSurface {
            DoublePrimaryButtons(
                startButtonText = "Primary",
                onStartButtonClick = { },
                endButtonText = "Secondary",
                onEndButtonClick = { }
            )
        }
    }
}
