package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.EmptyScreenIntent
import com.blockchain.home.presentation.allassets.EmptyScreenViewModel
import com.blockchain.home.presentation.allassets.EmptyScreenViewState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EmptyCard(
    onReceive: () -> Unit,
    assetActionsNavigation: AssetActionsNavigation,
    homeAssetsViewModel: AssetsViewModel,
    pkwActivityViewModel: PrivateKeyActivityViewModel,
    custodialActivityViewModel: CustodialActivityViewModel,
    viewModel: EmptyScreenViewModel = getViewModel(
        scope = payloadScope,
        parameters = {
            parametersOf(
                homeAssetsViewModel,
                pkwActivityViewModel,
                custodialActivityViewModel
            )
        }
    )
) {
    val viewState: EmptyScreenViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(EmptyScreenIntent.CheckEmptyState)
        onDispose { }
    }

    if (viewState.show.not())
        return

    when (viewState.mode) {
        WalletMode.CUSTODIAL -> CustodialEmptyStateCards(
            assetActionsNavigation  = assetActionsNavigation
        )
        WalletMode.NON_CUSTODIAL -> NonCustodialEmptyStateCard(
            onReceiveClicked  =  onReceive
        )
        else -> throw IllegalStateException("Wallet mode not supported")
    }
}
