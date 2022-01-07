package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.ExchangeSellButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Exchange Sell Button")
@Composable
fun ExchangeSellButtonPreview() {
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
fun ExchangeSellButtonDisabledPreview() {
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
fun ExchangeSellButtonLoadingPreview() {
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
