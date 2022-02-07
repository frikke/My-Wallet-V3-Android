package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Green700
import com.blockchain.componentlib.theme.Green900

@Composable
fun ExchangeBuyButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier,
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = AppTheme.colors.success,
        defaultBackgroundDarkColor = AppTheme.colors.success,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Green400,
        disabledBackgroundDarkColor = Green900,
        pressedBackgroundColor = Green700,
        modifier = modifier.requiredHeightIn(min = 48.dp),
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, _: ImageResource ->
            ButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha
            )
        },
    )
}

@Preview("Default", group = "Exchange buy button")
@Composable
private fun ExchangeBuyButtonPreview() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview("Disabled", group = "Exchange buy button")
@Preview
@Composable
private fun ExchangeBuyButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview("Loading", group = "Exchange buy button")
@Composable
private fun ExchangeBuyButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeBuyButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeBuyButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeBuyButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeBuyButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
