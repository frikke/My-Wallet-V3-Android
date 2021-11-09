package com.blockchain.componentlib.button

import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.divider.VerticalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Blue700
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DoublePrimaryButtons(
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
        ButtonState.Enabled -> if (isStartButtonPressed) Blue700 else Blue600
        ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
        ButtonState.Loading -> Blue600
    }

    val endButtonInteractionSource = remember { MutableInteractionSource() }
    val isEndButtonPressed = endButtonInteractionSource.collectIsPressedAsState().value
    val endButtonBackgroundColor = when (endButtonState) {
        ButtonState.Enabled -> if (isEndButtonPressed) Blue700 else Blue600
        ButtonState.Disabled -> if (isDarkTheme) Grey900 else Blue400
        ButtonState.Loading -> Blue600
    }

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
                backgroundColor = startButtonBackgroundColor,
                contentColor = Color.Unspecified,
                disabledBackgroundColor = startButtonBackgroundColor,
                disabledContentColor = Color.Unspecified,
            ),
            content = {
                FixedSizeButtonContent(
                    state = startButtonState,
                    text = startButtonText,
                    textColor = Color.White,
                    textAlpha = startTextAlpha,
                )
            }
        )

        val dividerAlpha = if (isStartButtonPressed.not() && isEndButtonPressed.not()) 1f else 0f
        VerticalDivider(
            dividerColor = Color.White,
            modifier = Modifier
                .background(Blue600)
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
            content = {
                FixedSizeButtonContent(
                    state = endButtonState,
                    text = endButtonText,
                    textColor = Color.White,
                    textAlpha = endTextAlpha,
                )
            }
        )
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
                onEndButtonClick = { },
            )
        }
    }
}
