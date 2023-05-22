package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.dapps.HomeDappsViewState
import com.blockchain.home.presentation.dapps.composable.WalletConnectDashboardCTA
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.composable.common.WalletConnectDappTableRow
import timber.log.Timber

internal fun LazyListScope.homeDapps(
    homeDappsState: HomeDappsViewState,
    onWalletConnectSeeAllSessionsClicked: () -> Unit,
    onDappSessionClicked: (DappSessionUiElement) -> Unit,
    openQrCodeScanner: () -> Unit,
) {

    if (homeDappsState !is HomeDappsViewState.Loading) {
        paddedItem(
            paddingValues = {
                PaddingValues(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    top = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.tinySpacing
                )
            }
        ) {
            // val analytics: Analytics = get() TODO add analytics
            TableRowHeader(
                title = "Connected Apps",
                actionOnClick = onWalletConnectSeeAllSessionsClicked,
                actionTitle = if (homeDappsState is HomeDappsViewState.HomeDappsSessions) {
                    "See All"
                } else {
                    null
                },
            )
        }
    }

    if (homeDappsState is HomeDappsViewState.NoSessions) {
        paddedItem(
            paddingValues = {
                PaddingValues(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.smallSpacing
                )
            }
        ) {
            WalletConnectDashboardCTA(openQrCodeScanner)
        }
    } else if (homeDappsState is HomeDappsViewState.HomeDappsSessions) {
        paddedRoundedCornersItems(
            items = homeDappsState.connectedSessions,
            paddingValues = {
                PaddingValues(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.smallSpacing
                )
            }
        ) { session ->
            WalletConnectDappTableRow(
                session = session,
                onSessionClicked = {
                    Timber.d("Session clicked: $session")
                    onDappSessionClicked(session)
                }
            )
        }
    }
}
