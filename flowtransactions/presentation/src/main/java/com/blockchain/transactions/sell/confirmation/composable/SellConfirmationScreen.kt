package com.blockchain.transactions.sell.confirmation.composable

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.AppDivider
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
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.AnnotatedStringUtils
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.urllinks.CHECKOUT_REFUND_POLICY
import com.blockchain.presentation.urllinks.NETWORK_ERC20_EXPLANATION
import com.blockchain.presentation.urllinks.NETWORK_FEE_EXPLANATION
import com.blockchain.presentation.urllinks.ORDER_PRICE_EXPLANATION
import com.blockchain.stringResources.R
import com.blockchain.transactions.common.confirmation.composable.ConfirmationSection
import com.blockchain.transactions.sell.SellAnalyticsEvents
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.confirmation.SellConfirmationIntent
import com.blockchain.transactions.sell.confirmation.SellConfirmationNavigation
import com.blockchain.transactions.sell.confirmation.SellConfirmationViewModel
import com.blockchain.transactions.sell.confirmation.SellConfirmationViewState
import com.blockchain.transactions.sell.neworderstate.composable.SellNewOrderStateArgs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.isLayer2Token
import java.math.BigInteger
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SellConfirmationScreen(
    args: SellConfirmationArgs,
    viewModel: SellConfirmationViewModel = getViewModel(
        scope = payloadScope,
        parameters = { parametersOf(args) }
    ),
    analytics: Analytics = get(),
    openNewOrderState: (SellNewOrderStateArgs) -> Unit,
    backClicked: () -> Unit
) {
    val sourceAccount = args.sourceAccount.data ?: return
    val targetAccount = args.targetAccount.data ?: return
    val sourceCryptoAmount = args.sourceCryptoAmount

    LaunchedEffect(Unit) {
        analytics.logEvent(SellAnalyticsEvents.ConfirmationViewed(sourceAccount.currency.networkTicker))
    }

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is SellConfirmationNavigation.NewOrderState -> openNewOrderState(event.args)
            null -> {}
        }
    }

    val state by viewModel.viewState.collectAsStateLifecycleAware()

    ConfirmationContent(
        state = state,
        submitOnClick = {
            viewModel.onIntent(SellConfirmationIntent.SubmitClicked)

            analytics.logEvent(
                SellAnalyticsEvents.ConfirmationSellClicked(
                    fromTicker = sourceAccount.currency.networkTicker,
                    fromAmount = sourceCryptoAmount.toStringWithSymbol(),
                    toTicker = targetAccount.currency.networkTicker,
                )
            )
        },
        backClicked = backClicked
    )
}

@Composable
private fun ConfirmationContent(
    state: SellConfirmationViewState,
    submitOnClick: () -> Unit,
    backClicked: () -> Unit
) {
    Column(
        modifier = Modifier.background(AppColors.background)
    ) {
        NavigationBar(
            title = stringResource(R.string.common_confirm),
            onBackButtonClick = backClicked
        )

        Box(Modifier.fillMaxHeight()) {
            Column(
                Modifier
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

                    AppDivider()

                    SellConfirmationTableRow(
                        startTitle = stringResource(R.string.common_from),
                        endTitle = state.sourceAsset.name,
                        onClick = null
                    )

                    AppDivider()

                    SellConfirmationTableRow(
                        startTitle = stringResource(R.string.common_to),
                        endTitle = state.targetAsset.name,
                        onClick = null
                    )

                    AppDivider()

                    if (state.sourceNetworkFeeFiatAmount != null) {
                        NetworkFee(state.sourceAsset, state.sourceNetworkFeeFiatAmount)
                        AppDivider()
                    }

                    SellConfirmationTableRow(
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
                text = stringResource(R.string.sell_confirmation_cta_button),
                state = state.submitButtonState,
                onClick = submitOnClick
            )
        }
    }
}

@Composable
fun SellDisclaimer() {
    val context = LocalContext.current
    val map = mapOf("refund_policy" to CHECKOUT_REFUND_POLICY)
    val disclaimer = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
        stringId = R.string.sell_confirmation_disclaimer,
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
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val formattedTime = DateUtils.formatElapsedTime(remainingSeconds.toLong())
        SimpleText(
            text = stringResource(R.string.tx_confirmation_quote_refresh_timer),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
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

    Column(Modifier.padding(AppTheme.dimensions.smallSpacing)) {
        Row(
            modifier = modifier
                .clickable {
                    isExplainerVisible = !isExplainerVisible
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleText(
                text = startTitle,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
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
}

@Composable
private fun SellExchangeRate(rate: ExchangeRate?) {
    val context = LocalContext.current
    val exchangeRateExplainer = rate?.let {
        val learnMoreString = AnnotatedStringUtils.getAnnotatedStringWithMappedAnnotations(
            context = context,
            stringId = R.string.common_linked_learn_more,
            linksMap = mapOf("learn_more_link" to ORDER_PRICE_EXPLANATION)
        )
        buildAnnotatedString {
            append(stringResource(R.string.checkout_item_price_blurb))
            append(" ")
            append(learnMoreString)
        }
    }

    if (exchangeRateExplainer != null) {
        ConfirmationExplainerTableRow(
            startTitle = stringResource(R.string.tx_confirmation_exchange_rate_label),
            endTitle = stringResource(
                R.string.tx_confirmation_exchange_rate_value,
                rate.from.displayTicker,
                rate.price.toStringWithSymbol()
            ),
            explainerText = exchangeRateExplainer,
        )
    } else {
        SellConfirmationTableRow(
            startTitle = stringResource(R.string.tx_confirmation_exchange_rate_label),
            endTitle = null,
            onClick = null,
        )
    }
}

@Composable
private fun NetworkFee(
    sourceAmountCurrency: AssetInfo,
    sourceNetworkFeeFiatAmount: FiatValue,
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
            R.string.checkout_one_fee_note,
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

    val feeAmount = if (!sourceNetworkFeeFiatAmount.isZero) {
        sourceNetworkFeeFiatAmount.toStringWithSymbol()
    } else {
        val str = FiatValue.fromMinor(sourceNetworkFeeFiatAmount.currency, BigInteger.valueOf(1))
            .toStringWithSymbol()
        "<$str"
    }

    ConfirmationExplainerTableRow(
        modifier = modifier,
        startTitle = stringResource(R.string.checkout_item_network_fee_label),
        endTitle = feeAmount,
        explainerText = exchangeRateExplainer,
    )
}

@Preview
@Composable
private fun PreviewInitialState() {
    val state = SellConfirmationViewState(
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
    val state = SellConfirmationViewState(
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