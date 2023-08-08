package com.blockchain.walletconnect.ui.composable

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.stringResources.R.string
import com.blockchain.walletconnect.R
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectSessionProposalState
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionProposalIntent
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionProposalViewModel
import com.blockchain.walletconnect.ui.dapps.v2.WalletConnectSessionProposalViewState
import info.blockchain.balance.CryptoCurrency
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun WalletConnectSessionProposal(
    sessionId: String,
    walletAddress: String,
    onDismiss: () -> Unit,
    analytics: Analytics = get()
) {

    val sessionProposalViewModel: WalletConnectSessionProposalViewModel = getViewModel(scope = payloadScope)
    val sessionProposalViewState: WalletConnectSessionProposalViewState by sessionProposalViewModel.viewState
        .collectAsStateLifecycleAware()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sessionProposalViewModel.onIntent(WalletConnectSessionProposalIntent.LoadSessionProposal(sessionId))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    WalletConnectSessionProposalScreen(
        viewState = sessionProposalViewState,
        walletAddress = walletAddress,
        onApprove = {
            analytics.logEvent(WalletConnectAnalytics.DappConnectionConfirmed)
            sessionProposalViewModel.onIntent(WalletConnectSessionProposalIntent.ApproveSession)
        },
        onReject = {
            analytics.logEvent(WalletConnectAnalytics.DappConnectionRejected)
            sessionProposalViewModel.onIntent(WalletConnectSessionProposalIntent.RejectSession)
        },
        onClosePress = {
            if (sessionProposalViewState.sessionState == null) {
                analytics.logEvent(WalletConnectAnalytics.DappConnectionRejected)
                sessionProposalViewModel.onIntent(WalletConnectSessionProposalIntent.RejectSession)
            }
            onDismiss()
        }
    )
}

@Composable
private fun WalletConnectSessionProposalScreen(
    viewState: WalletConnectSessionProposalViewState,
    walletAddress: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClosePress: () -> Unit,
) {
    Box(modifier = Modifier.background(AppTheme.colors.background)) {
        Column {

            SheetHeader(
                onClosePress = onClosePress,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.mediumSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val targetState: TargetState by remember(viewState.sessionState) {
                    mutableStateOf(
                        when (viewState.sessionState) {
                            null -> TargetState.UNDEFINED
                            WalletConnectSessionProposalState.APPROVED -> TargetState.SUCCESS
                            WalletConnectSessionProposalState.REJECTED -> TargetState.FAILURE
                        }
                    )
                }

                AnimatedStateIndicatorImage(
                    imageResource = viewState.dappLogoUrl.takeIf { it.isNotEmpty() }?.let {
                        ImageResource.Remote(it, shape = AppTheme.shapes.veryLarge)
                    } ?: ImageResource.Local(R.drawable.ic_walletconnect_logo),
                    state = targetState
                )

                SmallVerticalSpacer()

                AnimatedSessionProposalContent(
                    dappName = viewState.dappName,
                    dappDescription = viewState.dappDescription.ifEmpty {
                        stringResource(string.empty_dapp_description)
                    },
                    shortWalletAddress = "${walletAddress.take(4)}...${walletAddress.takeLast(4)}",
                    onApprove = onApprove,
                    onReject = onReject,
                    state = targetState
                )
            }
        }
    }
}

@Composable
fun WalletConnectSessionNotSupported(
    dappName: String,
    dappLogoUrl: String,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.background(AppColors.background)) {
        Column {

            SheetHeader(
                onClosePress = {
                    onDismiss()
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.mediumSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier.size(
                        width = 100.dp,
                        height = 100.dp
                    )
                ) {
                    Image(
                        imageResource = if (dappLogoUrl.isNotEmpty())
                            ImageResource.Remote(dappLogoUrl)
                        else ImageResource.Local(
                            R.drawable.ic_walletconnect_logo
                        ),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(AppTheme.shapes.veryLarge)
                            .align(Alignment.TopStart)
                    )

                    Image(
                        imageResource = Icons.Filled.Alert.withTint(AppTheme.colors.error),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AppColors.backgroundSecondary)
                            .border(
                                width = 6.dp,
                                color = AppColors.background,
                                shape = CircleShape
                            )
                            .align(Alignment.BottomEnd)
                    )
                }

                StandardVerticalSpacer()

                SimpleText(
                    text = stringResource(
                        string.walletconnect_session_rejected_title,
                        dappName.ifEmpty {
                            stringResource(string.empty_dapp_title)
                        }
                    ),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SmallVerticalSpacer()

                SimpleText(
                    text = stringResource(string.dapp_network_not_supported),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre
                )

                StandardVerticalSpacer()

                PrimaryButton(
                    text = stringResource(string.common_ok),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

enum class TargetState {
    UNDEFINED,
    SUCCESS,
    FAILURE
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedStateIndicatorImage(imageResource: ImageResource, state: TargetState) {
    Box(
        modifier = Modifier.size(
            width = 100.dp,
            height = 100.dp
        )
    ) {
        Image(
            imageResource = imageResource,
            modifier = Modifier
                .size(88.dp)
                .clip(AppTheme.shapes.veryLarge)
                .align(Alignment.TopStart)
        )

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(durationMillis = 300)
                ) with fadeOut(
                    animationSpec = tween(durationMillis = 300)
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) { targetState ->
            when (targetState) {
                TargetState.SUCCESS -> {
                    Image(
                        imageResource = Icons.Filled.Check.withTint(AppTheme.colors.success),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AppColors.backgroundSecondary)
                            .border(
                                width = 6.dp,
                                color = AppColors.background,
                                shape = CircleShape
                            )
                    )
                }

                TargetState.FAILURE -> {
                    Image(
                        imageResource = Icons.Filled.Alert.withTint(AppTheme.colors.error),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AppColors.backgroundSecondary)
                            .border(
                                width = 6.dp,
                                color = AppColors.background,
                                shape = CircleShape
                            )
                    )
                }

                else -> {
                    // No-op
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedSessionProposalContent(
    dappName: String,
    dappDescription: String,
    shortWalletAddress: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    state: TargetState
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            scaleIn(
                animationSpec = tween(
                    durationMillis = 300
                )
            ) with fadeOut(
                animationSpec = tween(durationMillis = 300)
            )
        }
    ) { targetState ->
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (targetState) {
                TargetState.UNDEFINED -> {

                    if (dappName.isNotEmpty()) {
                        SimpleText(
                            text = dappName,
                            style = ComposeTypographies.Title3,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Centre
                        )

                        SmallVerticalSpacer()
                    }

                    SimpleText(
                        text = dappDescription,
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )

                    LargeVerticalSpacer()

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            SimpleText(
                                text = stringResource(string.common_wallet),
                                style = ComposeTypographies.Caption1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Start
                            )

                            SimpleText(
                                text = shortWalletAddress,
                                style = ComposeTypographies.Body1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Start
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            SimpleText(
                                text = stringResource(string.common_network),
                                style = ComposeTypographies.Caption1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.End
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Image(
                                    imageResource = ImageResource.Local(
                                        com.blockchain.componentlib.R.drawable.ic_eth
                                    )
                                )

                                TinyHorizontalSpacer()

                                SimpleText(
                                    text = CryptoCurrency.ETHER.name,
                                    style = ComposeTypographies.Body1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.Start
                                )
                            }
                        }
                    }

                    SmallVerticalSpacer()

                    Row(modifier = Modifier.fillMaxWidth()) {
                        PrimaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(string.common_connect),
                            onClick = onApprove
                        )

                        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(string.common_cancel),
                            onClick = onReject
                        )
                    }
                }

                TargetState.SUCCESS -> {
                    SimpleText(
                        text = stringResource(
                            id = string.walletconnect_session_approved_title,
                            dappName.ifEmpty {
                                stringResource(
                                    id = string.empty_dapp_title
                                )
                            }
                        ),
                        style = ComposeTypographies.Title3,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )

                    StandardVerticalSpacer()
                }

                TargetState.FAILURE -> {
                    SimpleText(
                        text = stringResource(
                            id = string.walletconnect_session_rejected_title,
                            dappName.ifEmpty {
                                stringResource(
                                    id = string.empty_dapp_title
                                )
                            }
                        ),
                        style = ComposeTypographies.Title3,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )

                    SmallVerticalSpacer()

                    SimpleText(
                        text = stringResource(id = string.go_back_to_your_browser),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )

                    StandardVerticalSpacer()
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewWalletConnectSessionProposalScreen() {
    WalletConnectSessionProposalScreen(
        viewState = WalletConnectSessionProposalViewState(
            sessionState = null,
            dappName = "dapp name",
            dappDescription = "dapp description",
            dappLogoUrl = "logo"
        ),
        walletAddress = "address",
        onApprove = {},
        onReject = {},
        onClosePress = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWalletConnectSessionProposalScreenDark() {
    PreviewWalletConnectSessionProposalScreen()
}

@Preview
@Composable
fun PreviewWalletConnectSessionProposalScreenFailed() {
    WalletConnectSessionProposalScreen(
        viewState = WalletConnectSessionProposalViewState(
            sessionState = WalletConnectSessionProposalState.REJECTED,
            dappName = "dapp name",
            dappDescription = "dapp description",
            dappLogoUrl = "logo"
        ),
        walletAddress = "address",
        onApprove = {},
        onReject = {},
        onClosePress = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWalletConnectSessionProposalScreenFailedDark() {
    PreviewWalletConnectSessionProposalScreenFailed()
}

@Preview
@Composable
fun PreviewWalletConnectSessionProposalScreenApproved() {
    WalletConnectSessionProposalScreen(
        viewState = WalletConnectSessionProposalViewState(
            sessionState = WalletConnectSessionProposalState.APPROVED,
            dappName = "dapp name",
            dappDescription = "dapp description",
            dappLogoUrl = "logo"
        ),
        walletAddress = "address",
        onApprove = {},
        onReject = {},
        onClosePress = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWalletConnectSessionProposalScreenApprovedDark() {
    PreviewWalletConnectSessionProposalScreenApproved()
}

@Preview
@Composable
fun WalletConnectSessionNotSupportedPreview() {
    WalletConnectSessionNotSupported(
        dappName = "My Dapp",
        dappLogoUrl = "https://www.blockchain.com/static/img/logo.svg",
        onDismiss = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletConnectSessionNotSupportedPreviewDark() {
    WalletConnectSessionNotSupportedPreview()
}
