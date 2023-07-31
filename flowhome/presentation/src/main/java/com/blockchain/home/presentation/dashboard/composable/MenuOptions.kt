package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun MenuOptions(
    modifier: Modifier = Modifier,
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope),
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    showBackground: Boolean,
    showBalance: Boolean
) {
    val viewState: AssetsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    MenuOptionsScreen(
        modifier = modifier,
        walletBalance = (viewState.balance.balance as? DataResource.Data)?.data?.toStringWithSymbol() ?: "",
        openSettings = openSettings,
        launchQrScanner = launchQrScanner,
        showBackground = showBackground,
        showBalance = showBalance
    )
}
