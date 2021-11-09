package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ExchangeSplitButtons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "default", group = "Exchange split button")
@Composable
fun ExchangeSplitButtonPreview() {
    AppTheme {
        AppSurface {
            ExchangeSplitButtons(
                exchangeBuyButtonText = "Buy",
                exchangeBuyButtonOnClick = {},
                exchangeSellButtonText = "Sell",
                exchangeSellButtonOnClick = {},
            )
        }
    }
}