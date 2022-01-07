package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.ExchangeBuyButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview("Default", group = "Exchange buy button")
@Composable
fun ExchangeBuyButtonPreview() {
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
fun ExchangeBuyButtonDisabledPreview() {
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
fun ExchangeBuyButtonLoadingPreview() {
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
