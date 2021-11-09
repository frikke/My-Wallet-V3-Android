package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red700
import com.blockchain.componentlib.theme.Red900

@Composable
fun ExchangeSellButton(
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
        defaultBackgroundLightColor = AppTheme.colors.error,
        defaultBackgroundDarkColor = AppTheme.colors.error,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Red400,
        disabledBackgroundDarkColor = Red900,
        pressedBackgroundColor = Red700,
        modifier = modifier,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float ->
            FixedSizeButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                textAlpha = textAlpha
            )
        },
    )
}

@Preview(name = "Default", group = "Exchange Sell Button")
@Composable
private fun ExchangeSellButtonPreview() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Exchange Sell Button")
@Composable
private fun ExchangeSellButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Exchange Sell Button")
@Composable
private fun ExchangeSellButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeSellButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeSellButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExchangeSellButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            ExchangeSellButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
