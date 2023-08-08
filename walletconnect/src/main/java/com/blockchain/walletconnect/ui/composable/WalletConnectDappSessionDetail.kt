package com.blockchain.walletconnect.ui.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalErrorButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.ui.composable.common.DappSessionUiElement
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionDetailIntent
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionDetailViewModel
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionDetailViewState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun WalletConnectDappSessionDetail(
    sessionId: String,
    isV2: Boolean,
    onDismiss: () -> Unit,
    analytics: Analytics = get()
) {

    val sessionDetailViewModel: WalletConnectSessionDetailViewModel = getViewModel(scope = payloadScope)
    val sessionDetailViewState: WalletConnectSessionDetailViewState by sessionDetailViewModel.viewState
        .collectAsStateLifecycleAware()

    LaunchedEffect(Unit) {
        sessionDetailViewModel.onIntent(WalletConnectSessionDetailIntent.LoadSession(sessionId, isV2))
    }

    when (sessionDetailViewState) {
        is WalletConnectSessionDetailViewState.Loading -> {
            ShimmerLoadingCard()
            LargeVerticalSpacer()
        }

        is WalletConnectSessionDetailViewState.WalletConnectSessionLoaded -> {
            val session = (sessionDetailViewState as WalletConnectSessionDetailViewState.WalletConnectSessionLoaded)
                .session
            WalletConnectDappSessionManage(
                session = session,
                onDisconnectClicked = {
                    sessionDetailViewModel.onIntent(WalletConnectSessionDetailIntent.DisconnectSession)
                    analytics.logEvent(
                        WalletConnectAnalytics.ConnectedDappActioned(
                            dappName = session.dappName,
                            action = WalletConnectAnalytics.DappConnectionAction.DISCONNECT
                        )
                    )
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun WalletConnectDappSessionManage(
    session: DappSessionUiElement,
    onDisconnectClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.background(AppColors.background)
    ) {
        SheetHeader(
            onClosePress = onDismiss,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.smallSpacing
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageResource = session.dappLogoUrl.takeIf { it.isNotEmpty() }?.let {
                    ImageResource.Remote(it, size = 88.dp)
                } ?: ImageResource.Local(com.blockchain.walletconnect.R.drawable.ic_walletconnect_logo, size = 88.dp)
            )

            StandardVerticalSpacer()

            if (session.dappName.isNotEmpty()) {
                SimpleText(
                    text = session.dappName,
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SmallVerticalSpacer()

                SimpleText(
                    text = session.dappDescription,
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre
                )

                LargeVerticalSpacer()
            }

            MinimalErrorButton(
                text = stringResource(id = R.string.common_disconnect),
                onClick = onDisconnectClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun WalletConnectDappSessionManagePreview() {
    AppTheme {
        WalletConnectDappSessionManage(
            session = DappSessionUiElement(
                dappName = "My Dapp",
                dappDescription = "This is a description of my dapp",
                dappUrl = "https://mydapp.com",
                dappLogoUrl = "https://mydapp.com/logo.png",
                chainName = "Ethereum",
                chainLogo = "https://ethereum.org/logo.png",
                sessionId = "1234567890",
                isV2 = true
            ),
            onDisconnectClicked = {},
            onDismiss = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletConnectDappSessionManagePreviewDark() {
    WalletConnectDappSessionManagePreview()
}
