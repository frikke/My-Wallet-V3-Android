package com.blockchain.transactions.receive.network

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.presentation.urllinks.MULTICHAIN_LEARN_MORE
import com.blockchain.stringResources.R

@Composable
fun NetworkWarning(
    assetTicker: String,
    networkName: String,
    closeOnClick: () -> Unit
) {
    NetworkWarningScreen(
        assetTicker = assetTicker,
        networkName = networkName,
        closeOnClick = closeOnClick,
    )
}

@Composable
private fun NetworkWarningScreen(
    assetTicker: String,
    networkName: String,
    closeOnClick: () -> Unit
) {
    val context = LocalContext.current

    BottomSheetTwoButtons(
        title = stringResource(R.string.receive_network_alert_title, assetTicker, networkName),
        headerImageResource = null,
        subtitleAlign = TextAlign.Left,
        showTitleInHeader = true,
        subtitle = stringResource(R.string.receive_network_alert_subtitle, assetTicker, networkName),
        onCloseClick = closeOnClick,
        button1 = BottomSheetButton(
            type = ButtonType.MINIMAL,
            onClick = {
                context.openUrl(MULTICHAIN_LEARN_MORE)
            },
            text = stringResource(R.string.common_learn_more)
        ),
        button2 = BottomSheetButton(
            type = ButtonType.PRIMARY,
            onClick = closeOnClick,
            text = stringResource(R.string.common_ok)
        )
    )
}

@Preview
@Composable
private fun PreviewNetworkWarningScreen() {
    NetworkWarningScreen(
        assetTicker = "BTC",
        networkName = "Blockchain account",
        closeOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNetworkWarningScreenDark() {
    PreviewNetworkWarningScreen()
}
