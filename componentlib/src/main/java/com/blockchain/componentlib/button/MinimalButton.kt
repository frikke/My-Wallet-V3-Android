package com.blockchain.componentlib.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun MinimalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    state: ButtonState = ButtonState.Enabled
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val buttonTextAlpha = when (state) {
        ButtonState.Disabled -> 0.4f
        ButtonState.Loading -> 0f
        else -> 1f
    }

    val buttonBorderColor = when {
        isPressed && state == ButtonState.Enabled -> AppTheme.colors.primary
        else -> AppTheme.colors.medium
    }

    val spinnerAlpha = if (state == ButtonState.Loading) 1f else 0f

    OutlinedButton(
        onClick = onClick,
        enabled = state != ButtonState.Disabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isPressed) Color.White else Color.Transparent,
            contentColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified,
            disabledBackgroundColor = Color.Unspecified
        ),
        interactionSource = interactionSource,
        border = BorderStroke(width = 1.dp, color = buttonBorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(spinnerAlpha),
                color = AppTheme.colors.primary
            )
            Text(
                text = text,
                style = AppTheme.typography.body2,
                color = AppTheme.colors.primary,
                modifier = Modifier.alpha(buttonTextAlpha)
            )
        }
    }
}

@Preview
@Composable
fun MinimalButton_Basic() {
    AppTheme {
        Surface(color = Color.White) {
            MinimalButton(
                onClick = { },
                text = "Button"
            )
        }
    }
}

@Preview
@Composable
fun MinimalButton_Loading() {
    AppTheme {
        Surface(color = Color.White) {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview
@Composable
fun MinimalButton_Disabled() {
    AppTheme {
        Surface(color = Color.White) {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled,
            )
        }
    }
}

@Preview
@Composable
fun MinimalButton_FullWidth() {
    AppTheme {
        Surface(color = Color.White) {
            MinimalButton(
                onClick = { },
                text = "Max width",
                state = ButtonState.Disabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
