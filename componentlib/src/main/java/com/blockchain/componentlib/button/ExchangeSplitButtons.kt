package com.blockchain.componentlib.button

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ExchangeSplitButtons(
    exchangeBuyButtonText: String,
    exchangeBuyButtonOnClick: () -> Unit,
    exchangeSellButtonText: String,
    exchangeSellButtonOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    exchangeBuyButtonState: ButtonState = ButtonState.Enabled,
    exchangeSellButtonState: ButtonState = ButtonState.Enabled,
) {
    Row(modifier) {
        ExchangeBuyButton(
            text = exchangeBuyButtonText,
            onClick = exchangeBuyButtonOnClick,
            state = exchangeBuyButtonState,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExchangeSellButton(
            text = exchangeSellButtonText,
            onClick = exchangeSellButtonOnClick,
            state = exchangeSellButtonState,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(name = "default", group = "Split button")
@Composable
private fun ExchangeSplitButtonPreview() {
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
