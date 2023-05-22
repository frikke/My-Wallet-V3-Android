package com.blockchain.walletconnect.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.componentlib.R
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.MediumHorizontalSpacer
import com.blockchain.componentlib.theme.Pink700
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.composable.common.WalletConnectDappTableRow
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectDappListIntent
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectDappListViewModel
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectDappListViewState
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.getViewModel
import timber.log.Timber

@Composable
fun WalletConnectDappListScreen(
    onBackPressed: () -> Unit,
    onSessionClicked: (DappSessionUiElement) -> Unit,
) {
    val dappListViewModel: WalletConnectDappListViewModel = getViewModel(scope = payloadScope)
    val dappListViewState: WalletConnectDappListViewState by dappListViewModel.viewState.collectAsStateLifecycleAware()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dappListViewModel.onIntent(WalletConnectDappListIntent.LoadData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (dappListViewState) {
        is WalletConnectDappListViewState.Loading -> {
            ShimmerLoadingCard()
        }

        is WalletConnectDappListViewState.WalletConnectDappListSessions -> {
            WalletConnectDappList(
                sessions = (dappListViewState as WalletConnectDappListViewState.WalletConnectDappListSessions)
                    .connectedSessions,
                onSessionClicked = onSessionClicked,
                onBackPressed = onBackPressed,
                onDisconnectAllSessions = {
                    dappListViewModel.onIntent(WalletConnectDappListIntent.DisconnectAllSessions)
                    onBackPressed()
                }
            )
        }
    }
}

@Composable
fun WalletConnectDappList(
    sessions: List<DappSessionUiElement>,
    onSessionClicked: (DappSessionUiElement) -> Unit,
    onBackPressed: () -> Unit,
    onDisconnectAllSessions: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(contentAlignment = Alignment.TopEnd) {

            val expanded = remember { mutableStateOf(false) }
            NavigationBar(
                walletMode = WalletMode.NON_CUSTODIAL,
                mutedBg = true,
                title = stringResource(com.blockchain.stringResources.R.string.dapps_list_title),
                startNavigationBarButton = NavigationBarButton.Icon(
                    drawable = R.drawable.ic_nav_bar_back,
                    onIconClick = onBackPressed,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_back
                ),
                endNavigationBarButtons = listOf(
                    NavigationBarButton.DropdownMenu(
                        expanded = expanded,
                        onMenuClick = { expanded.value = !expanded.value },
                    ) {
                        DropdownMenuItem(onClick = onDisconnectAllSessions) {
                            MediumHorizontalSpacer()
                            Text(
                                text = stringResource(com.blockchain.stringResources.R.string.common_disconnect_all),
                                color = Pink700,
                                style = AppTheme.typography.title3
                            )
                            MediumHorizontalSpacer()
                        }
                    }
                ),
                spaceBetweenArrangement = true
            )
        }

        LazyColumn {
            paddedRoundedCornersItems(
                items = sessions,
                paddingValues = { PaddingValues(horizontal = AppTheme.dimensions.smallSpacing) }
            ) { session ->
                WalletConnectDappTableRow(
                    session = session,
                    onSessionClicked = {
                        Timber.d("Session clicked: $session")
                        onSessionClicked(session)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalletConnectDappsListPreview() {
    AppTheme {
        WalletConnectDappList(
            sessions = listOf(
                DappSessionUiElement(
                    dappName = "My Dapp",
                    dappDescription = "This is a description of my dapp",
                    dappUrl = "https://mydapp.com",
                    dappLogoUrl = "https://mydapp.com/logo.png",
                    chainName = "Ethereum",
                    chainLogo = "https://ethereum.org/logo.png",
                    sessionId = "1234567890",
                    isV2 = true
                ),
                DappSessionUiElement(
                    dappName = "My Dapp",
                    dappDescription = "This is a description of my dapp",
                    dappUrl = "https://mydapp.com",
                    dappLogoUrl = "https://mydapp.com/logo.png",
                    chainName = "Ethereum",
                    chainLogo = "https://ethereum.org/logo.png",
                    sessionId = "1234567890",
                    isV2 = true
                ),
                DappSessionUiElement(
                    dappName = "My Dapp",
                    dappDescription = "This is a description of my dapp",
                    dappUrl = "https://mydapp.com",
                    dappLogoUrl = "https://mydapp.com/logo.png",
                    chainName = "Ethereum",
                    chainLogo = "https://ethereum.org/logo.png",
                    sessionId = "1234567890",
                    isV2 = true
                ),
                DappSessionUiElement(
                    dappName = "My Dapp",
                    dappDescription = "This is a description of my dapp",
                    dappUrl = "https://mydapp.com",
                    dappLogoUrl = "https://mydapp.com/logo.png",
                    chainName = "Ethereum",
                    chainLogo = "https://ethereum.org/logo.png",
                    sessionId = "1234567890",
                    isV2 = true
                ),
                DappSessionUiElement(
                    dappName = "My Dapp",
                    dappDescription = "This is a description of my dapp",
                    dappUrl = "https://mydapp.com",
                    dappLogoUrl = "https://mydapp.com/logo.png",
                    chainName = "Ethereum",
                    chainLogo = "https://ethereum.org/logo.png",
                    sessionId = "1234567890",
                    isV2 = true
                ),
            ),
            onBackPressed = {},
            onDisconnectAllSessions = {},
            onSessionClicked = {}
        )
    }
}
