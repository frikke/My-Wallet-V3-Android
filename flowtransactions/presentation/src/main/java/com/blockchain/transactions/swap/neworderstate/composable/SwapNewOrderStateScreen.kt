package com.blockchain.transactions.swap.neworderstate.composable

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.isInternetConnectionError
import com.blockchain.betternavigation.NavContext
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.icons.Swap
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.checkValidUrlAndOpen
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.koin.payloadScope
import com.blockchain.outcome.doOnSuccess
import com.blockchain.transactions.swap.SwapAnalyticsEvents
import com.blockchain.transactions.swap.SwapGraph
import com.blockchain.transactions.swap.model.ShouldUpsellPassiveRewardsResult
import com.blockchain.transactions.upsell.interest.UpsellInterestArgs
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import java.io.Serializable
import org.koin.androidx.compose.get

sealed interface SwapNewOrderState : Serializable {
    object PendingDeposit : SwapNewOrderState
    object Succeeded : SwapNewOrderState
    data class Error(val error: Exception) : SwapNewOrderState
}

data class SwapNewOrderStateArgs(
    val sourceAmount: CryptoValue,
    val targetAmount: CryptoValue,
    val shouldLaunchUpsellPassiveRewards: Bindable<ShouldUpsellPassiveRewardsResult>,
    val orderState: SwapNewOrderState
) : Serializable

@Composable
fun NavContext.SwapNewOrderStateScreen(
    analytics: Analytics = get(),
    args: SwapNewOrderStateArgs,
    deeplinkRedirector: DeeplinkRedirector = get(scope = payloadScope),
    exitFlow: () -> Unit
) {
    val shouldLaunchUpsellPassiveRewards = args.shouldLaunchUpsellPassiveRewards.data ?: return
    val context = LocalContext.current
    var handleDeeplinkUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(handleDeeplinkUrl) {
        val url = handleDeeplinkUrl?.appendTickerToDeeplink(args.sourceAmount.currencyCode)
        handleDeeplinkUrl = null
        if (url != null) {
            deeplinkRedirector.processDeeplinkURL(url).awaitOutcome()
                .doOnSuccess {
                    if (it is DeepLinkResult.DeepLinkResultUnknownLink) {
                        it.uri?.let { uri ->
                            context.checkValidUrlAndOpen(uri)
                        }
                    }
                }
            exitFlow()
        }
    }

    LaunchedEffect(args.orderState) {
        when (args.orderState) {
            SwapNewOrderState.PendingDeposit -> analytics.logEvent(SwapAnalyticsEvents.PendingViewed)
            SwapNewOrderState.Succeeded -> analytics.logEvent(SwapAnalyticsEvents.SuccessViewed)
            is SwapNewOrderState.Error -> { /* n/a for now */ }
        }
    }

    NewOrderStateContent(
        sourceAmount = args.sourceAmount,
        targetAmount = args.targetAmount,
        orderState = args.orderState,
        handleDeeplinkUrlAndExit = { deeplinkUrl ->
            handleDeeplinkUrl = deeplinkUrl
        },
        doneClicked = {
            when (shouldLaunchUpsellPassiveRewards) {
                is ShouldUpsellPassiveRewardsResult.ShouldLaunch -> {
                    val upsellArgs = UpsellInterestArgs(
                        sourceAccount = Bindable(shouldLaunchUpsellPassiveRewards.sourceAccount),
                        targetAccount = Bindable(shouldLaunchUpsellPassiveRewards.targetAccount),
                        interestRate = shouldLaunchUpsellPassiveRewards.interestRate,
                    )
                    navigateTo(SwapGraph.UpsellInterest, upsellArgs)
                }
                ShouldUpsellPassiveRewardsResult.ShouldNot -> exitFlow()
            }
        }
    )
}

@Composable
private fun NewOrderStateContent(
    sourceAmount: CryptoValue,
    targetAmount: CryptoValue,
    orderState: SwapNewOrderState,
    handleDeeplinkUrlAndExit: (String) -> Unit,
    doneClicked: () -> Unit
) {
    Column(
        modifier = Modifier.background(AppTheme.colors.light)
    ) {
        Spacer(Modifier.weight(0.33f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val swapIcon = Icons.Swap
                .withTint(AppTheme.colors.title)
                .withBackground(
                    backgroundColor = White,
                    iconSize = 59.dp,
                    backgroundSize = 88.dp
                )
            val tagIcon = when (orderState) {
                SwapNewOrderState.PendingDeposit ->
                    Icons.Filled.Pending
                        .withTint(AppTheme.colors.muted)
                        .withSize(44.dp)
                SwapNewOrderState.Succeeded ->
                    Icons.Filled.Check
                        .withTint(AppTheme.colors.success)
                        .withSize(44.dp)
                is SwapNewOrderState.Error ->
                    errorStatusIcon(orderState)
            }

            val stackedIcon = StackedIcon.SmallTag(
                main = swapIcon,
                tag = tagIcon
            )

            SmallTagIcon(
                icon = stackedIcon,
                iconBackground = AppTheme.colors.light,
                mainIconSize = 88.dp,
                tagIconSize = 44.dp,
                borderColor = AppTheme.colors.light
            )

            SmallVerticalSpacer()

            val title = when (orderState) {
                SwapNewOrderState.PendingDeposit ->
                    stringResource(
                        com.blockchain.stringResources.R.string.swap_neworderstate_pending_deposit_title,
                        sourceAmount.currency.name
                    )
                SwapNewOrderState.Succeeded ->
                    stringResource(com.blockchain.stringResources.R.string.swap_neworderstate_succeeded_title)
                is SwapNewOrderState.Error ->
                    errorTitle(orderState)
            }
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                text = title,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            TinyHorizontalSpacer()

            val description = when (orderState) {
                SwapNewOrderState.PendingDeposit -> stringResource(
                    com.blockchain.stringResources.R.string.swap_neworderstate_pending_deposit_description,
                    sourceAmount.toStringWithSymbol(),
                    targetAmount.toStringWithSymbol(),
                    // TODO(aromano): usually X minutes
                    if (
                        sourceAmount.currency.networkTicker == "BTC" ||
                        targetAmount.currency.networkTicker == "BTC"
                    ) {
                        "30"
                    } else {
                        "10"
                    }
                )
                SwapNewOrderState.Succeeded -> stringResource(
                    com.blockchain.stringResources.R.string.swap_neworderstate_succeeded_description,
                    sourceAmount.toStringWithSymbol(),
                    targetAmount.toStringWithSymbol()
                )
                is SwapNewOrderState.Error ->
                    errorDescription(orderState)
            }
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.dimensions.standardSpacing),
                text = description,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        }

        Spacer(Modifier.weight(0.66f))

        if (orderState is SwapNewOrderState.Error) {
            ErrorCtaButtons(
                state = orderState,
                onCtaClicked = { deeplinkUrl ->
                    handleDeeplinkUrlAndExit(deeplinkUrl)
                }
            )
        } else {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing),
                text = stringResource(com.blockchain.stringResources.R.string.common_done),
                onClick = doneClicked
            )
        }
    }
}

@Composable
private fun errorStatusIcon(state: SwapNewOrderState.Error): ImageResource {
    val serverSideError = (state.error as? NabuApiException)?.getServerSideErrorInfo()
    val serverSideIcon = serverSideError?.statusUrl?.ifEmpty { null }

    return if (serverSideIcon != null) {
        ImageResource.Remote(
            url = serverSideIcon,
            shape = CircleShape,
            size = 44.dp
        )
    } else {
        Icons.Filled.Alert
            .withTint(AppTheme.colors.error)
            .withSize(44.dp)
    }
}

@Composable
private fun errorTitle(state: SwapNewOrderState.Error): String {
    val serverSideError = (state.error as? NabuApiException)?.getServerSideErrorInfo()
    val message = when {
        serverSideError != null -> serverSideError.title
        state.error.isInternetConnectionError() -> stringResource(
            com.blockchain.stringResources.R.string.executing_connection_error
        )
        else -> null
    }?.ifEmpty { null }

    return message ?: stringResource(com.blockchain.stringResources.R.string.something_went_wrong_try_again)
}

@Composable
private fun errorDescription(state: SwapNewOrderState.Error): String {
    val serverSideError = (state.error as? NabuApiException)?.getServerSideErrorInfo()
    val message = when {
        serverSideError != null -> serverSideError.description
        state.error is NabuApiException -> state.error.getErrorDescription()
        else -> null
    }?.ifEmpty { null }

    return message ?: stringResource(com.blockchain.stringResources.R.string.order_error_subtitle)
}

@Composable
private fun ErrorCtaButtons(
    state: SwapNewOrderState.Error,
    onCtaClicked: (deeplinkUrl: String) -> Unit
) {
    val serverSideError = (state.error as? NabuApiException)?.getServerSideErrorInfo()
    val actions = serverSideError?.actions.orEmpty()
    val modifier = Modifier
        .fillMaxWidth()
        .padding(AppTheme.dimensions.smallSpacing)

    if (actions.isNotEmpty()) {
        actions.forEachIndexed { index, action ->
            val title = action.title.ifEmpty { stringResource(com.blockchain.stringResources.R.string.common_ok) }
            val onClick = { onCtaClicked(action.deeplinkPath) }
            when (index) {
                0 -> PrimaryButton(modifier = modifier, text = title, onClick = onClick)
                1 -> SecondaryButton(modifier = modifier, text = title, onClick = onClick)
                2 -> MinimalPrimaryButton(modifier = modifier, text = title, onClick = onClick)
            }
        }
    } else {
        PrimaryButton(
            modifier = modifier,
            text = stringResource(com.blockchain.stringResources.R.string.common_ok),
            onClick = { onCtaClicked("") }
        )
    }
}

private fun String.appendTickerToDeeplink(currencyCode: String): Uri =
    Uri.parse("$this?code=$currencyCode")

@Preview
@Composable
private fun PreviewPendingDeposit() {
    NewOrderStateContent(
        sourceAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.5.toBigDecimal()),
        targetAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.05.toBigDecimal()),
        orderState = SwapNewOrderState.PendingDeposit,
        handleDeeplinkUrlAndExit = {},
        doneClicked = {}
    )
}

@Preview
@Composable
private fun PreviewSucceeded() {
    NewOrderStateContent(
        sourceAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.5.toBigDecimal()),
        targetAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.05.toBigDecimal()),
        orderState = SwapNewOrderState.Succeeded,
        handleDeeplinkUrlAndExit = {},
        doneClicked = {}
    )
}

@Preview
@Composable
private fun PreviewError() {
    val error = ServerSideUxErrorInfo(
        id = null,
        title = "Error title",
        description = "Error description",
        iconUrl = "",
        statusUrl = "",
        actions = listOf(
            ServerErrorAction("One", ""),
            ServerErrorAction("Two", "")
        ),
        categories = emptyList()
    )
    val apiException = NabuApiException(
        message = "some error",
        httpErrorCode = 400,
        errorType = null,
        errorCode = null,
        errorDescription = "nabu error description",
        path = null,
        id = null,
        serverSideUxError = error
    )
    NewOrderStateContent(
        sourceAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.5.toBigDecimal()),
        targetAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.05.toBigDecimal()),
        orderState = SwapNewOrderState.Error(apiException),
        handleDeeplinkUrlAndExit = {},
        doneClicked = {}
    )
}
