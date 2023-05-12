package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.walletconnect.ui.composable.WalletConnectDashboardCTA

internal fun LazyListScope.homeDapps(
    /*earnState: WalletConnectViewState,
    earnViewModel: WalletConnect,*/
    openQrCodeScanner: () -> Unit,
) {

    paddedItem(
        paddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        // val analytics: Analytics = get() TODO add analytics
        Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
        TableRowHeader(
            title = "Connected Apps",
            actionOnClick = {
                openQrCodeScanner()
            }
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    }

    paddedItem(
        paddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        WalletConnectDashboardCTA(openQrCodeScanner)
    }
}
