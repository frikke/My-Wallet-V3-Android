package com.blockchain.walletconnect.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R.string
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectAuthRequestIntent
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectAuthRequestViewModel
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectAuthRequestViewState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun WalletConnectAuthRequest(
    authId: String,
    onDismiss: () -> Unit,
    analytics: Analytics = get()
) {

    val authRequestViewModel: WalletConnectAuthRequestViewModel = getViewModel(scope = payloadScope)
    val authRequestViewState: WalletConnectAuthRequestViewState by authRequestViewModel.viewState
        .collectAsStateLifecycleAware()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authRequestViewModel.onIntent(WalletConnectAuthRequestIntent.LoadAuthRequest(authId))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.background(AppTheme.colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.mediumSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SheetHeader(
                shouldShowDivider = false,
                onClosePress = {
                    onDismiss()
                }
            )

            when (authRequestViewState) {
                is WalletConnectAuthRequestViewState.WalletConnectAuthRequestLoading -> {
                    CircularProgressBar()
                }

                is WalletConnectAuthRequestViewState.WalletConnectAuthRequestData -> {

                    (
                        authRequestViewState
                            as WalletConnectAuthRequestViewState.WalletConnectAuthRequestData
                        ).let { viewState ->

                        Image(
                            imageResource = ImageResource.Local(R.drawable.ic_walletconnect_logo),
                            modifier = Modifier
                                .size(88.dp)
                                .clip(AppTheme.shapes.veryLarge)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            SimpleText(
                                text = stringResource(string.walletconnect_auth_request_title),
                                style = ComposeTypographies.Title3,
                                color = ComposeColors.Title,
                                gravity = ComposeGravities.Centre
                            )

                            SmallVerticalSpacer()

                            SimpleText(
                                text = viewState.domain,
                                style = ComposeTypographies.Body1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Centre
                            )

                            StandardVerticalSpacer()

                            Column {
                                SimpleText(
                                    text = stringResource(string.common_message),
                                    style = ComposeTypographies.Caption1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.Start
                                )

                                TinyVerticalSpacer()

                                Box(modifier = Modifier.background(Color.White, AppTheme.shapes.medium)) {
                                    SimpleText(
                                        text = viewState.authMessage,
                                        style = ComposeTypographies.Body1,
                                        color = ComposeColors.Body,
                                        gravity = ComposeGravities.Start,
                                        modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)
                                    )
                                }
                            }

                            LargeVerticalSpacer()

                            Row(modifier = Modifier.fillMaxWidth()) {
                                PrimaryButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(string.common_connect),
                                    onClick = {
                                        authRequestViewModel.onIntent(WalletConnectAuthRequestIntent.ApproveAuth)
                                        onDismiss()
                                    }
                                )

                                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                                SecondaryButton(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(string.common_cancel),
                                    onClick = {
                                        authRequestViewModel.onIntent(WalletConnectAuthRequestIntent.ApproveAuth)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
