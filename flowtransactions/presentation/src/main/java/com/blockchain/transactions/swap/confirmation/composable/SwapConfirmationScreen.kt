package com.blockchain.transactions.swap.confirmation.composable

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.card.TwoAssetAction
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.ChevronDown
import com.blockchain.componentlib.icons.ChevronUp
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.AnnotatedStringUtils
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.urllinks.CHECKOUT_REFUND_POLICY
import com.blockchain.presentation.urllinks.EXCHANGE_SWAP_RATE_EXPLANATION
import com.blockchain.transactions.common.confirmation.composable.ConfirmationSection
import com.blockchain.transactions.common.confirmation.composable.ConfirmationTableRow
import com.blockchain.transactions.swap.SwapAnalyticsEvents
import com.blockchain.transactions.swap.SwapAnalyticsEvents.Companion.accountType
import com.blockchain.transactions.swap.confirmation.SwapConfirmationArgs
import com.blockchain.transactions.swap.confirmation.SwapConfirmationIntent
import com.blockchain.transactions.swap.confirmation.SwapConfirmationNavigation
import com.blockchain.transactions.swap.confirmation.SwapConfirmationViewModel
import com.blockchain.transactions.swap.confirmation.SwapConfirmationViewState
import com.blockchain.transactions.swap.neworderstate.composable.SwapNewOrderStateArgs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SwapConfirmationScreen(
    args: SwapConfirmationArgs,
    viewModel: SwapConfirmationViewModel = getViewModel(
        scope = payloadScope,
        parameters = { parametersOf(args) }
    ),
    analytics: Analytics = get(),
    openNewOrderState: (SwapNewOrderStateArgs) -> Unit,
    backClicked: () -> Unit
) {
    val sourceAccount = args.sourceAccount.data ?: return
    val targetAccount = args.targetAccount.data ?: return
    val sourceCryptoAmount = args.sourceCryptoAmount

    LaunchedEffect(Unit) {
        analytics.logEvent(SwapAnalyticsEvents.ConfirmationViewed)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is SwapConfirmationNavigation.NewOrderState -> openNewOrderState(event.args)
            null -> {}
        }
    }

    val state by viewModel.viewState.collectAsStateLifecycleAware()

    ConfirmationContent(
        state = state,
        submitOnClick = {
            viewModel.onIntent(SwapConfirmationIntent.SubmitClicked)

            analytics.logEvent(
                SwapAnalyticsEvents.SwapClicked(
                    fromTicker = sourceAccount.currency.networkTicker,
                    fromAmount = sourceCryptoAmount.toStringWithSymbol(),
                    toTicker = targetAccount.currency.networkTicker,
                    destination = targetAccount.accountType()
                )
            )
        },
        backClicked = backClicked
    )
}

@Composable
private fun ConfirmationContent(
    state: SwapConfirmationViewState,
    submitOnClick: () -> Unit,
    backClicked: () -> Unit
) {
    Column(
        modifier = Modifier.background(AppColors.background)
    ) {
        NavigationBar(
            title = stringResource(com.blockchain.stringResources.R.string.swap_confirmation_navbar),
            onBackButtonClick = backClicked
        )

        Box(Modifier.fillMaxHeight()) {
            Column(
                Modifier
                    .padding(AppTheme.dimensions.smallSpacing)
                    .verticalScroll(rememberScrollState())
            ) {
                val topIcon = if (state.sourceNativeAssetIconUrl != null) {
                    StackedIcon.SmallTag(
                        ImageResource.Remote(state.sourceAsset.logo),
                        ImageResource.Remote(state.sourceNativeAssetIconUrl)
                    )
                } else {
                    StackedIcon.SingleIcon(ImageResource.Remote(state.sourceAsset.logo))
                }

                val bottomIcon = if (state.targetNativeAssetIconUrl != null) {
                    StackedIcon.SmallTag(
                        ImageResource.Remote(state.targetAsset.logo),
                        ImageResource.Remote(state.targetNativeAssetIconUrl)
                    )
                } else {
                    StackedIcon.SingleIcon(ImageResource.Remote(state.targetAsset.logo))
                }

                TwoAssetAction(
                    topTitle = state.sourceAsset.name,
                    topSubtitle = state.sourceAssetDescription,
                    topEndTitle = state.sourceCryptoAmount.toStringWithSymbol(),
                    topEndSubtitle = state.sourceFiatAmount?.toStringWithSymbol().orEmpty(),
                    topIcon = topIcon,
                    bottomTitle = state.targetAsset.name,
                    bottomSubtitle = state.targetAssetDescription,
                    bottomEndTitle = state.targetCryptoAmount?.toStringWithSymbol().orEmpty(),
                    bottomEndSubtitle = state.targetFiatAmount?.toStringWithSymbol().orEmpty(),
                    bottomIcon = bottomIcon
                )

                StandardVerticalSpacer()

                SwapExchangeRate(state.sourceToTargetExchangeRate)

                StandardVerticalSpacer()

                if (
                    (state.sourceNetworkFeeCryptoAmount != null && !state.sourceNetworkFeeCryptoAmount.isZero) ||
                    (state.targetNetworkFeeCryptoAmount != null && !state.targetNetworkFeeCryptoAmount.isZero)
                ) {
                    NetworkFees(
                        sourceNetworkFeeCryptoAmount = state.sourceNetworkFeeCryptoAmount,
                        sourceNetworkFeeFiatAmount = state.sourceNetworkFeeFiatAmount,
                        targetNetworkFeeCryptoAmount = state.targetNetworkFeeCryptoAmount,
                        targetNetworkFeeFiatAmount = state.targetNetworkFeeFiatAmount
                    )

                    StandardVerticalSpacer()
                }

                SwapQuoteTimer(
                    remainingSeconds = state.quoteRefreshRemainingSeconds ?: 90,
                    remainingPercentage = state.quoteRefreshRemainingPercentage ?: 1f
                )

                StandardVerticalSpacer()

                SwapDisclaimer()

                // Padding for the CTA
                Spacer(Modifier.height(AppTheme.dimensions.epicSpacing))
            }

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing)
                    .align(Alignment.BottomCenter),
                text = stringResource(com.blockchain.stringResources.R.string.common_swap),
                state = state.submitButtonState,
                onClick = submitOnClick
            )
        }

    }
}

@Composable
fun SwapDisclaimer() {
    val context = LocalContext.current
    val map = mapOf("refund_policy" to CHECKOUT_REFUND_POLICY)
    val disclaimer = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
        stringId = com.blockchain.stringResources.R.string.swap_confirmation_disclaimer_1,
        linksMap = map,
        context = context
    )

    SimpleText(
        modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
        text = disclaimer,
        style = ComposeTypographies.Caption1,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Centre,
        onAnnotationClicked = { tag, value ->
            if (tag == AnnotatedStringUtils.TAG_URL) {
                Intent(Intent.ACTION_VIEW, Uri.parse(value))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    .also { context.startActivity(it) }
            }
        }
    )
}

@Composable
fun SwapQuoteTimer(remainingSeconds: Int, remainingPercentage: Float, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val formattedTime = DateUtils.formatElapsedTime(remainingSeconds.toLong())
        SimpleText(
            text = stringResource(com.blockchain.stringResources.R.string.tx_confirmation_quote_refresh_timer),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )

        CircularProgressBar(
            modifier = Modifier
                .padding(start = AppTheme.dimensions.smallestSpacing)
                .size(AppTheme.dimensions.mediumSpacing),
            progress = remainingPercentage
        )

        Spacer(Modifier.weight(1f))

        SimpleText(
            text = formattedTime,
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )
    }
}

@Composable
private fun SwapExchangeRate(rate: ExchangeRate?, modifier: Modifier = Modifier) {
    var isExplainerVisible by remember { mutableStateOf(false) }
    Column(
        Modifier
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .clickable {
                isExplainerVisible = !isExplainerVisible
            }
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.tx_confirmation_exchange_rate_label),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            Image(
                modifier = Modifier.padding(start = AppTheme.dimensions.smallestSpacing),
                imageResource = Icons.Filled.Question
                    .withSize(AppTheme.dimensions.smallSpacing)
                    .withTint(AppTheme.colors.medium)
            )

            Spacer(Modifier.weight(1f))

            if (rate != null) {
                SimpleText(
                    text = stringResource(
                        com.blockchain.stringResources.R.string.tx_confirmation_exchange_rate_value,
                        rate.from.displayTicker,
                        rate.price.toStringWithSymbol()
                    ),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Start
                )
            }
        }

        if (isExplainerVisible) {
            val context = LocalContext.current
            val learnMoreString = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
                context = context,
                stringId = com.blockchain.stringResources.R.string.common_linked_learn_more,
                linksMap = mapOf("learn_more_link" to EXCHANGE_SWAP_RATE_EXPLANATION)
            )
            val explainerString = buildAnnotatedString {
                append(
                    stringResource(
                        com.blockchain.stringResources.R.string.checkout_swap_exchange_note,
                        rate!!.to.symbol,
                        rate.from.symbol
                    )
                )
                append(" ")
                append(learnMoreString)
            }
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.tinySpacing),
                text = explainerString,
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start,
                onAnnotationClicked = { tag, value ->
                    if (tag == AnnotatedStringUtils.TAG_URL) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(value))
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            .also { context.startActivity(it) }
                    }
                }
            )
        }
    }
}

@Composable
private fun NetworkFees(
    sourceNetworkFeeCryptoAmount: CryptoValue?,
    sourceNetworkFeeFiatAmount: FiatValue?,
    targetNetworkFeeCryptoAmount: CryptoValue?,
    targetNetworkFeeFiatAmount: FiatValue?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    ConfirmationSection(modifier = modifier) {
        val fiatCurrency = sourceNetworkFeeFiatAmount?.currency ?: targetNetworkFeeFiatAmount?.currency
        val totalNetworkFeesFiatAmount = fiatCurrency?.let {
            (sourceNetworkFeeFiatAmount ?: FiatValue.zero(fiatCurrency)) +
                (targetNetworkFeeFiatAmount ?: FiatValue.zero(fiatCurrency))
        }
        val expandedIcon = if (isExpanded) Icons.ChevronUp else Icons.ChevronDown
        ConfirmationTableRow(
            startTitle = stringResource(com.blockchain.stringResources.R.string.checkout_item_network_fee_label),
            endTitle = totalNetworkFeesFiatAmount?.toStringWithSymbol()?.withApproximationPrefix(),
            endImageResource = expandedIcon.withTint(AppTheme.colors.muted),
            onClick = {
                isExpanded = !isExpanded
            }
        )

        if (isExpanded) {
            if (sourceNetworkFeeCryptoAmount != null && !sourceNetworkFeeCryptoAmount.isZero) {
                HorizontalDivider(Modifier.fillMaxWidth())

                ConfirmationTableRow(
                    startTitle = stringResource(
                        com.blockchain.stringResources.R.string.checkout_item_network_fee,
                        sourceNetworkFeeCryptoAmount.currency.displayTicker
                    ),
                    endTitle = sourceNetworkFeeFiatAmount?.toStringWithSymbol()?.withApproximationPrefix(),
                    endByline = sourceNetworkFeeCryptoAmount.toStringWithSymbol(),
                    onClick = null
                )
            }

            if (targetNetworkFeeCryptoAmount != null && !targetNetworkFeeCryptoAmount.isZero) {
                HorizontalDivider(Modifier.fillMaxWidth())

                ConfirmationTableRow(
                    startTitle = stringResource(
                        com.blockchain.stringResources.R.string.checkout_item_network_fee,
                        targetNetworkFeeCryptoAmount.currency.displayTicker
                    ),
                    endTitle = targetNetworkFeeFiatAmount?.toStringWithSymbol()?.withApproximationPrefix(),
                    endByline = targetNetworkFeeCryptoAmount.toStringWithSymbol(),
                    onClick = null
                )
            }
        }
    }
}

private fun String?.withApproximationPrefix() = if (this != null) "~ $this" else null

@Preview
@Composable
private fun PreviewInitialState() {
    val state = SwapConfirmationViewState(
        isFetchQuoteLoading = true,
        sourceAsset = CryptoCurrency.ETHER,
        sourceNativeAssetIconUrl = null,
        sourceAssetDescription = "DeFi Wallet",
        targetAsset = CryptoCurrency.BTC,
        targetNativeAssetIconUrl = null,
        targetAssetDescription = "BTC",
        sourceCryptoAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.05.toBigDecimal()),
        sourceFiatAmount = null,
        targetCryptoAmount = null,
        targetFiatAmount = null,
        sourceToTargetExchangeRate = null,
        sourceNetworkFeeCryptoAmount = null,
        sourceNetworkFeeFiatAmount = null,
        targetNetworkFeeCryptoAmount = null,
        targetNetworkFeeFiatAmount = null,
        quoteRefreshRemainingPercentage = null,
        quoteRefreshRemainingSeconds = null,
        submitButtonState = ButtonState.Disabled
    )
    ConfirmationContent(
        state = state,
        submitOnClick = {},
        backClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewInitialStateDark() {
    PreviewInitialState()
}

@Preview
@Composable
private fun PreviewLoadedState() {
    val state = SwapConfirmationViewState(
        isFetchQuoteLoading = false,
        sourceAsset = CryptoCurrency.ETHER,
        sourceNativeAssetIconUrl = null,
        sourceAssetDescription = "DeFi Wallet",
        targetAsset = CryptoCurrency.BTC,
        targetNativeAssetIconUrl = null,
        targetAssetDescription = "BTC",
        sourceCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.ETHER, 1234567890.toBigDecimal()),
        sourceFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 100.0.toBigDecimal()),
        targetCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.BTC, 1234567.toBigInteger()),
        targetFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 96.12.toBigDecimal()),
        sourceToTargetExchangeRate = ExchangeRate(
            rate = 12345678.0.toBigDecimal(),
            to = CryptoCurrency.ETHER,
            from = CryptoCurrency.BTC
        ),
        sourceNetworkFeeCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.ETHER, 123456.toBigDecimal()),
        sourceNetworkFeeFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 1.0.toBigDecimal()),
        targetNetworkFeeCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.BTC, 12345.toBigInteger()),
        targetNetworkFeeFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 6.12.toBigDecimal()),
        quoteRefreshRemainingPercentage = 0.5f,
        quoteRefreshRemainingSeconds = 45,
        submitButtonState = ButtonState.Enabled
    )
    ConfirmationContent(
        state = state,
        submitOnClick = {},
        backClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLoadedStateDark() {
    PreviewLoadedState()
}