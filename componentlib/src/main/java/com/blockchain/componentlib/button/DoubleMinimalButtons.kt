package com.blockchain.componentlib.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.divider.VerticalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark300
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey700

@Composable
fun DoubleMinimalButtons(
    startButtonText: String,
    onStartButtonClick: () -> Unit,
    endButtonText: String,
    onEndButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    startButtonState: ButtonState = ButtonState.Enabled,
    endButtonState: ButtonState = ButtonState.Enabled,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {

    val startButtonInteractionSource = remember { MutableInteractionSource() }
    val isStartButtonPressed = startButtonInteractionSource.collectIsPressedAsState().value
    val startButtonBackgroundColor = when (startButtonState) {
        ButtonState.Enabled -> if (isStartButtonPressed) {
            AppTheme.colors.light
        } else {
            if (isDarkTheme) Color.Transparent else Color.White
        }
        ButtonState.Disabled, ButtonState.Loading -> {
            if (isDarkTheme) Color.Transparent else Color.White
        }
    }

    val endButtonInteractionSource = remember { MutableInteractionSource() }
    val isEndButtonPressed = endButtonInteractionSource.collectIsPressedAsState().value
    val endButtonBackgroundColor = when (endButtonState) {
        ButtonState.Enabled -> if (isEndButtonPressed) {
            AppTheme.colors.light
        } else {
            if (isDarkTheme) Color.Transparent else Color.White
        }
        ButtonState.Disabled, ButtonState.Loading -> {
            if (isDarkTheme) Color.Transparent else Color.White
        }
    }

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

    var borderColor = when (startButtonState) {
        ButtonState.Enabled -> if (isStartButtonPressed || isEndButtonPressed) {
            AppTheme.colors.primary
        } else {
            if (isDarkTheme) Dark300 else Grey100
        }
        ButtonState.Disabled -> if (isDarkTheme) Grey700 else Grey000
        ButtonState.Loading -> if (isDarkTheme) Dark300 else Grey100
    }

    Row(
        modifier = modifier
            .requiredHeightIn(min = 48.dp)
            .height(IntrinsicSize.Max)
            .border(
                border = BorderStroke(1.dp, borderColor),
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
                backgroundColor = startButtonBackgroundColor,
                contentColor = Color.Unspecified,
                disabledBackgroundColor = startButtonBackgroundColor,
                disabledContentColor = Color.Unspecified,
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            content = {
                FixedSizeButtonContent(
                    state = startButtonState,
                    text = startButtonText,
                    textColor = startTextColor,
                    textAlpha = startTextAlpha,
                    loadingIconResId = loadingIcon,
                )
            }
        )

        val dividerAlpha = if (isStartButtonPressed.not() && isEndButtonPressed.not()) 1f else 0f
        val dividerBackgroundColor = if (isDarkTheme) Color.Transparent else Color.White
        val dividerColor = if (isDarkTheme) Dark300 else Grey100
        VerticalDivider(
            dividerColor = dividerColor,
            modifier = Modifier
                .background(dividerBackgroundColor)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .alpha(dividerAlpha)
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
                backgroundColor = endButtonBackgroundColor,
                contentColor = Color.Unspecified,
                disabledBackgroundColor = endButtonBackgroundColor,
                disabledContentColor = Color.Unspecified,
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            content = {
                FixedSizeButtonContent(
                    state = endButtonState,
                    text = endButtonText,
                    textColor = endTextColor,
                    textAlpha = endTextAlpha,
                    loadingIconResId = loadingIcon,
                )
            }
        )
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
