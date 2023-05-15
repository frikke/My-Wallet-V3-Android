package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.dapps.HomeDappsViewState
import com.blockchain.home.presentation.dapps.composable.WalletConnectDashboardCTA
import com.blockchain.walletconnect.ui.composable.DappSessionUiElement
import com.blockchain.walletconnect.ui.composable.WalletConnectDappTableRow

internal fun LazyListScope.homeDapps(
    homeDappsState: HomeDappsViewState,
    onSessionClicked: (DappSessionUiElement) -> Unit,
    openQrCodeScanner: () -> Unit,
) {

    if (homeDappsState !is HomeDappsViewState.Loading) {
        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            // val analytics: Analytics = get() TODO add analytics
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            TableRowHeader(
                title = "Connected Apps",
                actionOnClick = {
                    // TODO: See All clicked
                }
            )
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }
    }

    if (homeDappsState is HomeDappsViewState.NoSessions) {
        paddedItem(paddingValues = PaddingValues(horizontal = 16.dp)) {
            WalletConnectDashboardCTA(openQrCodeScanner)
        }
    } else if (homeDappsState is HomeDappsViewState.HomeDappsSessions) {
        paddedRoundedCornersItems(
            items = homeDappsState.connectedSessions,
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) { session ->
            WalletConnectDappTableRow(
                session = session,
                onSessionClicked = { onSessionClicked(session) }
            )
        }
    }
}
