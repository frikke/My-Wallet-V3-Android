package com.blockchain.transactions.receive.rotatingaddress

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.stringResources.R

@Composable
fun RotatingAddress(
    assetTicker: String,
    accountLabel: String,
    closeOnClick: () -> Unit
) {
    RotatingAddressScreen(
        assetTicker = assetTicker,
        accountLabel = accountLabel,
        closeOnClick = closeOnClick,
    )
}

@Composable
private fun RotatingAddressScreen(
    assetTicker: String,
    accountLabel: String,
    closeOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
    ) {
        SheetHeader(
            title = stringResource(id = R.string.common_did_you_know),
            onClosePress = closeOnClick
        )

        Column(
            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
        ) {
            Text(
                text = stringResource(R.string.receive_rotating_address_desc, assetTicker, accountLabel),
                style = AppTheme.typography.paragraph1,
                color = AppColors.body
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_ok),
                onClick = closeOnClick
            )
        }
    }
}

@Preview
@Composable
private fun PreviewRotatingAddressScreen() {
    RotatingAddressScreen(
        assetTicker = "BTC",
        accountLabel = "Blockchain account",
        closeOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRotatingAddressScreenDark() {
    PreviewRotatingAddressScreen()
}
