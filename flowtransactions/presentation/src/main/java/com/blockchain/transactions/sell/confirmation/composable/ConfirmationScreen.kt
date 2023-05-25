package com.blockchain.transactions.sell.confirmation.composable

import android.content.Intent
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.AnnotatedStringUtils
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.urllinks.CHECKOUT_REFUND_POLICY
import com.blockchain.presentation.urllinks.EXCHANGE_SWAP_RATE_EXPLANATION
import com.blockchain.presentation.urllinks.NETWORK_ERC20_EXPLANATION
import com.blockchain.presentation.urllinks.NETWORK_FEE_EXPLANATION
import com.blockchain.stringResources.R
import com.blockchain.transactions.common.confirmation.composable.ConfirmationSection
import com.blockchain.transactions.common.confirmation.composable.ConfirmationTableRow
import com.blockchain.transactions.sell.confirmation.ConfirmationIntent
import com.blockchain.transactions.sell.confirmation.ConfirmationNavigation
import com.blockchain.transactions.sell.confirmation.ConfirmationViewModel
import com.blockchain.transactions.sell.confirmation.ConfirmationViewState
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.neworderstate.composable.NewOrderStateArgs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.isLayer2Token
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConfirmationScreen(
    args: SellConfirmationArgs,
    viewModel: ConfirmationViewModel = getViewModel(
        scope = payloadScope,
        parameters = { parametersOf(args) }
    ),
    analytics: Analytics = get(),
    openNewOrderState: (NewOrderStateArgs) -> Unit,
    backClicked: () -> Unit
) {
    val sourceAccount = args.sourceAccount.data ?: return
    val targetAccount = args.targetAccount.data ?: return
    val sourceCryptoAmount = args.sourceCryptoAmount

    LaunchedEffect(Unit) {
//        analytics.logEvent(SellAnalyticsEvents.ConfirmationViewed)
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is ConfirmationNavigation.NewOrderState -> openNewOrderState(event.args)
            null -> {}
        }
    }

    val state by viewModel.viewState.collectAsStateLifecycleAware()

    Column {
        NavigationBar(
            title = stringResource(R.string.swap_confirmation_navbar),
            onBackButtonClick = backClicked
        )

        ConfirmationContent(
            state = state,
            submitOnClick = {
                viewModel.onIntent(ConfirmationIntent.SubmitClicked)

//                analytics.logEvent(
//                    SellAnalyticsEvents.SwapClicked(
//                        fromTicker = sourceAccount.currency.networkTicker,
//                        fromAmount = sourceCryptoAmount.toStringWithSymbol(),
//                        toTicker = targetAccount.currency.networkTicker,
//                        destination = targetAccount.accountType()
//                    )
//                )
            }
        )
    }
}

@Composable
private fun ConfirmationContent(
    state: ConfirmationViewState,
    submitOnClick: () -> Unit
) {
    Box(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .background(AppTheme.colors.light)
                .padding(AppTheme.dimensions.smallSpacing)
                .verticalScroll(rememberScrollState())
        ) {
            TinyVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = state.targetFiatAmount?.toStringWithSymbol().orEmpty(),
                style = ComposeTypographies.Title1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
            )

            TinyVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = state.sourceCryptoAmount.toStringWithSymbol(),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
            )

            StandardVerticalSpacer()

            ConfirmationSection {
                SellExchangeRate(state.sourceToTargetExchangeRate)

                HorizontalDivider(Modifier.fillMaxWidth())

                ConfirmationTableRow(
                    startTitle = stringResource(R.string.common_from),
                    endTitle = state.sourceAsset.name,
                    onClick = null
                )

                HorizontalDivider(Modifier.fillMaxWidth())

                ConfirmationTableRow(
                    startTitle = stringResource(R.string.common_to),
                    endTitle = state.targetAsset.name,
                    onClick = null
                )

                HorizontalDivider(Modifier.fillMaxWidth())

                if (state.sourceNetworkFeeFiatAmount != null) {
                    NetworkFee(state.sourceAsset, state.sourceNetworkFeeFiatAmount)

                    HorizontalDivider(Modifier.fillMaxWidth())
                }

                ConfirmationTableRow(
                    startTitle = stringResource(R.string.common_total),
                    endTitle = state.totalFiatAmount?.toStringWithSymbol().orEmpty(),
                    endByline = state.totalCryptoAmount?.toStringWithSymbol().orEmpty(),
                    onClick = null
                )
            }

            StandardVerticalSpacer()

            SellQuoteTimer(
                remainingSeconds = state.quoteRefreshRemainingSeconds ?: 90,
                remainingPercentage = state.quoteRefreshRemainingPercentage ?: 1f
            )

            StandardVerticalSpacer()

            SellDisclaimer()

            // Padding for the CTA
            Spacer(Modifier.height(AppTheme.dimensions.epicSpacing))
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
                .align(Alignment.BottomCenter),
            text = stringResource(R.string.common_swap),
            state = state.submitButtonState,
            onClick = submitOnClick
        )
    }
}

@Composable
fun SellDisclaimer() {
    val context = LocalContext.current
    val map = mapOf("refund_policy" to CHECKOUT_REFUND_POLICY)
    val disclaimer = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
        stringId = R.string.swap_confirmation_disclaimer_1,
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
fun SellQuoteTimer(remainingSeconds: Int, remainingPercentage: Float, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val formattedTime = DateUtils.formatElapsedTime(remainingSeconds.toLong())
        SimpleText(
            text = stringResource(R.string.tx_confirmation_quote_refresh_timer),
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
private fun ConfirmationExplainerTableRow(
    startTitle: String,
    endTitle: String?,
    explainerText: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    var isExplainerVisible by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.clickable {
            isExplainerVisible = !isExplainerVisible
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleText(
            text = startTitle,
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

        if (endTitle != null) {
            SimpleText(
                text = endTitle,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }

    if (isExplainerVisible) {
        val context = LocalContext.current
        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.tinySpacing),
            text = explainerText,
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

@Composable
private fun SellExchangeRate(rate: ExchangeRate?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val learnMoreString = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
        context = context,
        stringId = R.string.common_linked_learn_more,
        linksMap = mapOf("learn_more_link" to EXCHANGE_SWAP_RATE_EXPLANATION)
    )
    val exchangeRateExplainer = buildAnnotatedString {
        append(
            stringResource(
                R.string.checkout_swap_exchange_note,
                rate!!.to.symbol,
                rate.from.symbol
            )
        )
        append(" ")
        append(learnMoreString)
    }

    ConfirmationExplainerTableRow(
        modifier = modifier,
        startTitle = stringResource(R.string.tx_confirmation_exchange_rate_label),
        endTitle = rate?.let {
            stringResource(
                R.string.tx_confirmation_exchange_rate_value,
                rate.from.displayTicker,
                rate.price.toStringWithSymbol()
            )
        },
        explainerText = exchangeRateExplainer,
    )
}

@Composable
private fun NetworkFee(
    sourceAmountCurrency: AssetInfo,
    sourceNetworkFeeFiatAmount: FiatValue?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exchangeRateExplainer = if (!sourceAmountCurrency.isLayer2Token) {
        val mainText = stringResource(
            R.string.checkout_one_fee_note,
            sourceAmountCurrency.name
        )
        val learnMoreString = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
            context = context,
            stringId = R.string.checkout_fee_link,
            linksMap = mapOf("learn_more_link" to NETWORK_FEE_EXPLANATION)
        )

        buildAnnotatedString {
            append(mainText)
            append(" ")
            append(learnMoreString)
        }
    } else {
        val mainText = stringResource(
            R.string.checkout_one_erc_20_fee_note,
            sourceAmountCurrency.coinNetwork!!.name,
            sourceAmountCurrency.name
        )
        val learnMoreString = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
            context = context,
            stringId = R.string.checkout_fee_link,
            linksMap = mapOf("learn_more_link" to NETWORK_ERC20_EXPLANATION)
        )

        buildAnnotatedString {
            append(mainText)
            append(" ")
            append(learnMoreString)
        }
    }

    ConfirmationExplainerTableRow(
        modifier = modifier,
        startTitle = stringResource(R.string.checkout_item_network_fee_label),
        endTitle = sourceNetworkFeeFiatAmount?.toStringWithSymbol(),
        explainerText = exchangeRateExplainer,
    )
}

@Preview
@Composable
private fun PreviewInitialState() {
    val state = ConfirmationViewState(
        isFetchQuoteLoading = true,
        sourceAsset = CryptoCurrency.ETHER,
        targetAsset = FiatCurrency.Dollars,
        sourceCryptoAmount = CryptoValue.fromMajor(CryptoCurrency.ETHER, 0.05.toBigDecimal()),
        targetFiatAmount = null,
        sourceToTargetExchangeRate = null,
        sourceNetworkFeeFiatAmount = null,
        totalFiatAmount = null,
        totalCryptoAmount = null,
        quoteRefreshRemainingPercentage = null,
        quoteRefreshRemainingSeconds = null,
        submitButtonState = ButtonState.Disabled
    )
    Column {
        ConfirmationContent(
            state = state,
            submitOnClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewLoadedState() {
    val state = ConfirmationViewState(
        isFetchQuoteLoading = false,
        sourceAsset = CryptoCurrency.ETHER,
        targetAsset = FiatCurrency.Dollars,
        sourceCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.ETHER, 1234567890.toBigDecimal()),
        targetFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 96.12.toBigDecimal()),
        sourceToTargetExchangeRate = ExchangeRate(
            rate = 12345678.0.toBigDecimal(),
            to = FiatCurrency.Dollars,
            from = CryptoCurrency.BTC
        ),
        sourceNetworkFeeFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 3.22.toBigDecimal()),
        totalFiatAmount = FiatValue.fromMajor(FiatCurrency.Dollars, 97.34.toBigDecimal()),
        totalCryptoAmount = CryptoValue.fromMinor(CryptoCurrency.ETHER, 2344567890.toBigDecimal()),
        quoteRefreshRemainingPercentage = 0.5f,
        quoteRefreshRemainingSeconds = 45,
        submitButtonState = ButtonState.Enabled
    )
    Column {
        ConfirmationContent(
            state = state,
            submitOnClick = {}
        )
    }
}
