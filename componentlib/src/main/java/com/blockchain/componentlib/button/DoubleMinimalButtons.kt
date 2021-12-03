package com.blockchain.componentlib.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.divider.VerticalDivider
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
fun DoubleMinimalButtons(
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
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {

    val startButtonInteractionSource = remember { MutableInteractionSource() }
    var startButtonBackgroundColor by remember(isDarkTheme) {
        mutableStateOf(if (isDarkTheme) Color.Transparent else Color.White)
    }

    val endButtonInteractionSource = remember { MutableInteractionSource() }
    var endButtonBackgroundColor by remember {
        mutableStateOf(if (isDarkTheme) Color.Transparent else Color.White)
    }

    var borderColor by remember {
        mutableStateOf(
            when (startButtonState) {
                ButtonState.Enabled -> if (isDarkTheme) Dark300 else Grey100
                ButtonState.Disabled -> if (isDarkTheme) Grey700 else Grey000
                ButtonState.Loading -> if (isDarkTheme) Dark300 else Grey100
            }
        )
    }

    var dividerAlpha by remember { mutableStateOf(1f) }

    val startTextAlpha = when (startButtonState) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) 1f else 0.7f
        ButtonState.Loading -> 0f
    }
    val endTextAlpha = when (endButtonState) {
        ButtonState.Enabled -> 1f
        ButtonState.Disabled -> if (isDarkTheme) 1f else 0.7f
        ButtonState.Loading -> 0f
    }
    val startTextColor = if (startButtonState == ButtonState.Disabled && isDarkTheme) {
        Grey600
    } else {
        AppTheme.colors.primary
    }
    val endTextColor = if (endButtonState == ButtonState.Disabled && isDarkTheme) {
        Grey600
    } else {
        AppTheme.colors.primary
    }
    val loadingIcon = if (isDarkTheme) {
        R.drawable.ic_loading_minimal_dark
    } else {
        R.drawable.ic_loading_minimal_light
    }

    LaunchedEffect(startButtonInteractionSource, startButtonState, isDarkTheme) {

        fun cancel() {
            startButtonBackgroundColor = if (isDarkTheme) Color.Transparent else Color.White
            borderColor = when (startButtonState) {
                ButtonState.Enabled -> if (isDarkTheme) Dark300 else Grey100
                ButtonState.Disabled -> if (isDarkTheme) Grey700 else Grey000
                ButtonState.Loading -> if (isDarkTheme) Dark300 else Grey100
            }
            dividerAlpha = 1f
        }

        startButtonInteractionSource.interactions.collectPressInteractions(
            onPressed = {
                if (startButtonState == ButtonState.Enabled) {
                    startButtonBackgroundColor = if (isDarkTheme) Dark800 else Grey000
                    borderColor = if (isDarkTheme) Blue400 else Blue600
                    dividerAlpha = 0f
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

    LaunchedEffect(endButtonInteractionSource, endButtonState, isDarkTheme) {

        fun cancel() {
            endButtonBackgroundColor = if (isDarkTheme) Color.Transparent else Color.White
            borderColor = when (endButtonState) {
                ButtonState.Enabled -> if (isDarkTheme) Dark300 else Grey100
                ButtonState.Disabled -> if (isDarkTheme) Grey700 else Grey000
                ButtonState.Loading -> if (isDarkTheme) Dark300 else Grey100
            }
            dividerAlpha = 1f
        }

        endButtonInteractionSource.interactions.collectPressInteractions(
            onPressed = {
                if (endButtonState == ButtonState.Enabled) {
                    endButtonBackgroundColor = if (isDarkTheme) Dark800 else Grey000
                    borderColor = if (isDarkTheme) Blue400 else Blue600
                    dividerAlpha = 0f
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
        Row(
            modifier = modifier
                .requiredHeightIn(min = 48.dp)
                .height(IntrinsicSize.Max)
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = animateColorAsState(targetValue = borderColor).value
                    ),
                    shape = AppTheme.shapes.small
                )
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
                    disabledContentColor = Color.Unspecified,
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                content = {
                    ButtonContent(
                        state = startButtonState,
                        text = startButtonText,
                        textColor = startTextColor,
                        contentAlpha = startTextAlpha,
                        loadingIconResId = loadingIcon,
                        icon = startButtonIcon,
                    )
                }
            )

            val dividerBackgroundColor = if (isDarkTheme) Color.Transparent else Color.White
            val dividerColor = if (isDarkTheme) Dark300 else Grey100
            VerticalDivider(
                dividerColor = dividerColor,
                modifier = Modifier
                    .background(animateColorAsState(targetValue = dividerBackgroundColor).value)
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
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = animateColorAsState(targetValue = endButtonBackgroundColor).value,
                    contentColor = Color.Unspecified,
                    disabledBackgroundColor = animateColorAsState(targetValue = endButtonBackgroundColor).value,
                    disabledContentColor = Color.Unspecified,
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                content = {
                    ButtonContent(
                        state = endButtonState,
                        text = endButtonText,
                        textColor = endTextColor,
                        contentAlpha = endTextAlpha,
                        loadingIconResId = loadingIcon,
                        icon = endButtonIcon,
                    )
                }
            )
        }
    }
}

@Preview(name = "Default", group = "Double minimal buttons")
@Composable
private fun DoubleMinimalButtons() {
    AppTheme {
        AppSurface {
            DoubleMinimalButtons(
                startButtonText = "Primary",
                onStartButtonClick = { },
                endButtonText = "Secondary",
                onEndButtonClick = { },
            )
        }
    }
}
