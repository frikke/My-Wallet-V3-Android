package com.dex.presentation.confirmation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.anim.AnimatedAmountCounter
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Error
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.dex.presentation.AmountFieldConfig
import com.dex.presentation.DexAnalyticsEvents
import com.dex.presentation.DexTxSubscribeScreen
import com.dex.presentation.SendAndReceiveAmountFields
import com.dex.presentation.graph.DexDestination
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun DexConfirmationScreen(
    onBackPressed: () -> Unit,
    navController: NavController,
    viewModel: DexConfirmationViewModel = getViewModel(scope = payloadScope),
    analytics: Analytics = get(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DexTxSubscribeScreen(
        subscribe = { viewModel.onIntent(ConfirmationIntent.SubscribeForTxUpdates) },
        unsubscribe = { viewModel.onIntent(ConfirmationIntent.UnSubscribeToTxUpdates) }
    )

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest { event ->
            when (event) {
                ConfirmationNavigationEvent.TxInProgressNavigationEvent -> navController.navigate(
                    route = DexDestination.InProgress.route
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(ConfirmationIntent.LoadTransactionData)
            }
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onIntent(ConfirmationIntent.StopListeningForUpdates)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        NavigationBar(
            title = stringResource(com.blockchain.stringResources.R.string.confirm_swap),
            onBackButtonClick = onBackPressed
        )

        val viewState: ConfirmationScreenViewState by viewModel.viewState.collectAsStateLifecycleAware()
        (viewState as? ConfirmationScreenViewState.DataConfirmationViewState)?.let { dataState ->

            LaunchedEffect(Unit) {
                with(dataState) {
                    analytics.logEvent(
                        DexAnalyticsEvents.PreviewViewed(
                            inputTicker = sellCurrency?.networkTicker.orEmpty(),
                            inputAmount = sellAmount?.toStringWithoutSymbol().orEmpty(),
                            outputTicker = buyCurrency?.networkTicker.orEmpty(),
                            outputAmount = buyAmount?.toStringWithoutSymbol().orEmpty(),
                            minOutputAmount = minAmount?.value?.toStringWithoutSymbol().orEmpty(),
                            slippage = slippage?.toString().orEmpty(),
                            networkFee = networkFee?.value?.toStringWithoutSymbol().orEmpty(),
                            networkFeeTicker = networkFee?.value?.currency?.networkTicker.orEmpty(),
                            blockchainFee = blockchainFee?.value?.toStringWithoutSymbol().orEmpty(),
                            blockchainFeeTicker = blockchainFee?.value?.currency?.networkTicker.orEmpty(),
                            inputNetwork = sellCurrency?.takeIf { it.isLayer2Token }?.coinNetwork?.networkTicker
                                ?: sellCurrency?.networkTicker.orEmpty(),
                            outputNetwork = buyCurrency?.takeIf { it.isLayer2Token }?.coinNetwork?.networkTicker
                                ?: dataState.sellCurrency?.networkTicker.orEmpty()
                        )
                    )
                }
            }

            var animatedState by remember {
                mutableStateOf(dataState.toAnimatedState())
            }

            LaunchedEffect(key1 = dataState) {
                animatedState = dataState.toAnimatedState()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    Modifier
                        .padding(
                            start = AppTheme.dimensions.smallSpacing,
                            end = AppTheme.dimensions.smallSpacing,
                            top = AppTheme.dimensions.smallSpacing,
                            bottom = AppTheme.dimensions.smallestSpacing
                        )
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        SendAndReceiveAmountFields(
                            sendAmountFieldConfig = AmountFieldConfig(
                                isReadOnly = true,
                                onValueChanged = { },
                                isEnabled = true,
                                exchange = dataState.exchangeSellAmount,
                                currency = dataState.sellCurrency,
                                max = null,
                                shouldAnimateChanges = true,
                                canChangeCurrency = false,
                                onCurrencyClicked = { },
                                amount = dataState.sellAmount,
                                balance = dataState.sellAccountBalance
                            ),

                            receiveAmountFieldConfig = AmountFieldConfig(
                                isReadOnly = true,
                                isEnabled = true,
                                shouldAnimateChanges = true,
                                canChangeCurrency = false,
                                onValueChanged = { },
                                exchange = dataState.buyExchangeAmount,
                                currency = dataState.buyCurrency,
                                max = null,
                                onCurrencyClicked = { },
                                amount = dataState.buyAmount,
                                balance = dataState.buyAccountBalance
                            )
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))
                    }
                    item {
                        Card(
                            backgroundColor = AppTheme.colors.backgroundSecondary,
                            shape = RoundedCornerShape(
                                topStart = AppTheme.dimensions.mediumSpacing,
                                topEnd = AppTheme.dimensions.mediumSpacing
                            ),
                            modifier = Modifier.padding(bottom = 1.dp),
                            elevation = 0.dp
                        ) {
                            dataState.network?.let {
                                NetworkConfirmation(it)
                                AppDivider()
                            }
                        }
                    }
                    item {
                        animatedState.confirmationExchangeRate?.let {
                            ExchangeRateConfirmation(it)
                            AppDivider()
                        }
                    }

                    dataState.slippage?.let { sl ->
                        item {
                            SlippageConfirmation(
                                sl = sl,
                                extraInfoOnClick = { destination ->
                                    navController.navigate(
                                        destination
                                    )
                                }
                            )
                            AppDivider()
                        }
                    }

                    item {
                        animatedState.minAmount?.let {
                            MinAmountConfirmation(
                                minAmount = it,
                                slippage = dataState.slippage ?: 0.0,
                                extraInfoOnClick = { destination ->
                                    navController.navigate(
                                        destination
                                    )
                                }
                            )
                            AppDivider()
                        }
                    }
                    item {
                        animatedState.networkFee?.let {
                            NetworkFee(
                                networkFee = it,
                                extraInfoOnClick = { destination ->
                                    navController.navigate(
                                        destination
                                    )
                                }
                            )
                            AppDivider()
                        }
                    }
                    item {
                        animatedState.bcdcFee?.let {
                            Card(
                                backgroundColor = AppTheme.colors.backgroundSecondary,
                                shape = RoundedCornerShape(
                                    bottomEnd = AppTheme.dimensions.mediumSpacing,
                                    bottomStart = AppTheme.dimensions.mediumSpacing
                                ),
                                elevation = 0.dp
                            ) {
                                BlockchainFee(
                                    fee = it,
                                    extraInfoOnClick = { destination ->
                                        navController.navigate(
                                            destination
                                        )
                                    }
                                )
                            }
                        }
                    }
                    dataState.minAmount?.let { minAmount ->
                        item {
                            SimpleText(
                                modifier = Modifier.padding(
                                    top = AppTheme.dimensions.smallSpacing,
                                    bottom = AppTheme.dimensions.smallSpacing
                                ),
                                text = stringResource(
                                    id = com.blockchain.stringResources.R.string.min_amount_estimation,
                                    minAmount.value.toStringWithSymbol()
                                ),
                                style = ComposeTypographies.Caption1,
                                color = ComposeColors.Body,
                                gravity = ComposeGravities.Centre
                            )
                        }
                    }
                }
                ConfirmationPinnedBottom(
                    newPriceAvailableAndNotAccepted = dataState.newPriceAvailable,
                    confirm = {
                        viewModel.onIntent(ConfirmationIntent.ConfirmSwap)

                        with(dataState) {
                            analytics.logEvent(
                                DexAnalyticsEvents.ConfirmSwapClicked(
                                    inputTicker = sellCurrency?.networkTicker.orEmpty(),
                                    inputAmount = sellAmount?.toStringWithoutSymbol().orEmpty(),
                                    outputTicker = buyCurrency?.networkTicker.orEmpty(),
                                    outputAmount = buyAmount?.toStringWithoutSymbol().orEmpty(),
                                    minOutputAmount = minAmount?.value?.toStringWithoutSymbol().orEmpty(),
                                    slippage = slippage?.toString().orEmpty(),
                                    networkFee = networkFee?.value?.toStringWithoutSymbol().orEmpty(),
                                    networkFeeTicker = networkFee?.value?.currency?.networkTicker.orEmpty(),
                                    blockchainFee = blockchainFee?.value?.toStringWithoutSymbol().orEmpty(),
                                    blockchainFeeTicker = blockchainFee?.value?.currency?.networkTicker.orEmpty(),
                                    inputNetwork = sellCurrency?.takeIf { it.isLayer2Token }
                                        ?.coinNetwork?.networkTicker ?: sellCurrency?.networkTicker.orEmpty(),
                                    outputNetwork = buyCurrency?.takeIf { it.isLayer2Token }
                                        ?.coinNetwork?.networkTicker ?: dataState.sellCurrency?.networkTicker.orEmpty()
                                )
                            )
                        }
                    },
                    accept = {
                        viewModel.onIntent(ConfirmationIntent.AcceptPrice)
                    },
                    state = when {
                        dataState.operationInProgress -> ButtonState.Loading
                        dataState.newPriceAvailable -> ButtonState.Disabled
                        dataState.errors.isNotEmpty() -> ButtonState.Disabled
                        else -> ButtonState.Enabled
                    },
                    alertMessage = (dataState.alertError)?.message(
                        LocalContext.current
                    ) ?: (dataState.commonUiError)?.let {
                        StringBuilder()
                            .appendLine(
                                it.title ?: stringResource(
                                    id = com.blockchain.stringResources.R.string.common_http_error_title
                                )
                            )
                            .append(
                                it.description ?: stringResource(
                                    id = com.blockchain.stringResources.R.string.common_http_error_description
                                )
                            )
                            .toString()
                    }
                )
            }
        }
    }
}

private fun ConfirmationScreenViewState.DataConfirmationViewState.toAnimatedState(): AnimatedConfirmationState {
    return AnimatedConfirmationState(
        confirmationExchangeRate = safeLet(dexExchangeRate, sellCurrency, buyCurrency) { rate, input, output ->
            ConfirmationExchangeRate(
                rate = rate,
                inputCurrency = input,
                outputCurrency = output
            )
        },
        minAmount = minAmount,
        networkFee = networkFee,
        bcdcFee = blockchainFee
    )
}

@Composable
private fun ConfirmationPinnedBottom(
    newPriceAvailableAndNotAccepted: Boolean,
    confirm: () -> Unit,
    accept: () -> Unit,
    state: ButtonState,
    alertMessage: String?
) {
    Column(
        modifier = Modifier
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(
                    topStart = AppTheme.dimensions.smallSpacing,
                    topEnd = AppTheme.dimensions.smallSpacing,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp
                )
            )
            .padding(
                all = AppTheme.dimensions.smallSpacing
            )

    ) {
        if (newPriceAvailableAndNotAccepted) {
            PriceUpdateWarning(accept)
        }

        if (alertMessage != null) {
            AlertButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing))
                    .fillMaxWidth(),
                text = alertMessage,
                onClick = { },
                state = ButtonState.Enabled
            )
        } else {
            SwapButton(
                onClick = confirm,
                state = state
            )
        }
    }
}

@Composable
private fun PriceUpdateWarning(accept: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.smallSpacing)
            .background(
                color = AppColors.background,
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)
            )
            .padding(all = AppTheme.dimensions.smallSpacing),
        verticalAlignment = CenterVertically
    ) {
        Image(
            imageResource = Icons.Filled.Error.withTint(AppColors.warning)
                .withSize(AppTheme.dimensions.largeSpacing)
        )
        SimpleText(
            modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing),
            text = stringResource(id = com.blockchain.stringResources.R.string.price_updated),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(id = com.blockchain.stringResources.R.string.accept),
            onClick = accept
        )
    }
}

@Composable
private fun SwapButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    state: ButtonState = ButtonState.Enabled
) {
    PrimaryButton(
        modifier = modifier
            .fillMaxWidth(),
        state = state,
        text = stringResource(id = com.blockchain.stringResources.R.string.common_swap),
        onClick = onClick
    )
}

@Composable
private fun NetworkFee(
    networkFee: ConfirmationScreenExchangeAmount,
    extraInfoOnClick: (String) -> Unit
) {
    val extraInfoTitle = stringResource(id = com.blockchain.stringResources.R.string.network_fee)
    val extraInfoDescription =
        stringResource(
            id = com.blockchain.stringResources.R.string.network_fee_info_description,
            networkFee.value.currency.displayTicker
        )
    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = com.blockchain.stringResources.R.string.network_fee),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = DexDestination.DexExtraInfoSheet.routeWithTitleAndDescription(
                            title = extraInfoTitle,
                            description = extraInfoDescription
                        )
                        extraInfoOnClick(
                            destination
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedAmountCounter(
                        amountText = networkFee.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    AnimatedAmountCounter(
                        amountText = "~ ${networkFee.exchange.toStringWithSymbol()}",
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                }
            }
        }
    )
}

@Composable
private fun BlockchainFee(
    fee: ConfirmationScreenExchangeAmount,
    extraInfoOnClick: (String) -> Unit
) {
    val extraInfoTitle = stringResource(id = com.blockchain.stringResources.R.string.bcdc_fee)
    val extraInfoDescription =
        stringResource(id = com.blockchain.stringResources.R.string.bcdc_fee_extra_info_description)
    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = com.blockchain.stringResources.R.string.bcdc_fee),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = DexDestination.DexExtraInfoSheet.routeWithTitleAndDescription(
                            title = extraInfoTitle,
                            description = extraInfoDescription
                        )
                        extraInfoOnClick(
                            destination
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedAmountCounter(
                        amountText = fee.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    AnimatedAmountCounter(
                        amountText = "~ ${fee.exchange.toStringWithSymbol()}",
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                }
            }
        }
    )
}

@Composable
private fun MinAmountConfirmation(
    minAmount: ConfirmationScreenExchangeAmount,
    slippage: Double,
    extraInfoOnClick: (String) -> Unit
) {
    val extraInfoTitle = stringResource(id = com.blockchain.stringResources.R.string.minimum_amount)
    val extraInfoDescription =
        stringResource(
            id = com.blockchain.stringResources.R.string.minimum_amount_extra_info,
            slippage.toPercentageString()
        )

    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = com.blockchain.stringResources.R.string.min_amount),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = DexDestination.DexExtraInfoSheet.routeWithTitleAndDescription(
                            title = extraInfoTitle,
                            description = extraInfoDescription
                        )
                        extraInfoOnClick(
                            destination
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedAmountCounter(
                        amountText = minAmount.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    AnimatedAmountCounter(
                        amountText = "~ ${minAmount.exchange.toStringWithSymbol()}",
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                }
            }
        }
    )
}

@Composable
private fun ExtraInfoIndicator(
    onClick: () -> Unit
) {
    Image(
        modifier = Modifier
            .padding(horizontal = AppTheme.dimensions.smallestSpacing)
            .clickableNoEffect { onClick() },
        imageResource = Icons.Question.withTint(AppColors.muted).withSize(14.dp)
    )
}

@Composable
private fun ExchangeRateConfirmation(confirmationExchangeRate: ConfirmationExchangeRate) {
    TableRow(
        content = {
            SimpleText(
                text = stringResource(id = com.blockchain.stringResources.R.string.exchange_rate),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
            Spacer(modifier = Modifier.weight(1f))
            AnimatedAmountCounter(
                amountText = "${confirmationExchangeRate.rate} " +
                    "${confirmationExchangeRate.outputCurrency.displayTicker} / ${
                    Money.fromMajor(
                        confirmationExchangeRate.inputCurrency,
                        BigDecimal.ONE
                    ).toStringWithSymbol(includeDecimalsWhenWhole = false)
                    }",
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.End
            )
        }
    )
}

@Preview
@Composable
private fun SlippageConfirmation(sl: Double = 1.0, extraInfoOnClick: (String) -> Unit = {}) {
    val extraInfoTitle = stringResource(id = com.blockchain.stringResources.R.string.allowed_slippage)
    val extraInfoDescription =
        stringResource(
            id = com.blockchain.stringResources.R.string.slippage_explanation,
        )
    TableRow(
        content = {
            Row(verticalAlignment = CenterVertically) {
                SimpleText(
                    text = stringResource(id = com.blockchain.stringResources.R.string.allowed_slippage),
                    style = ComposeTypographies.Paragraph2,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )

                ExtraInfoIndicator {
                    val destination = DexDestination.DexExtraInfoSheet.routeWithTitleAndDescription(
                        title = extraInfoTitle,
                        description = extraInfoDescription
                    )
                    extraInfoOnClick(
                        destination
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            SimpleText(
                text = sl.toPercentageString(),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    )
}

@Composable
private fun NetworkConfirmation(network: String) {
    TableRow(
        content = {
            SimpleText(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_network),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
            Spacer(modifier = Modifier.weight(1f))
            SimpleText(
                text = network,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    )
}

@Preview
@Composable
private fun PreviewPriceUpdateWarning() {
    PriceUpdateWarning(accept = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPriceUpdateWarningDark() {
    PreviewPriceUpdateWarning()
}

@Preview
@Composable
fun MinAmountPreview() {
    AppTheme {
        MinAmountConfirmation(
            minAmount = ConfirmationScreenExchangeAmount(
                value = Money.fromMinor(CryptoCurrency.ETHER, BigInteger("23123122312")),
                exchange = Money.fromMinor(CryptoCurrency.ETHER, BigInteger("2312"))
            ),
            slippage = 1.0
        ) { _ -> }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MinAmountPreviewDark() {
    MinAmountPreview()
}

private fun Double.toPercentageString(): String {
    val percentage = this * 100.0
    return "%,.2f%%".format(percentage)
}

private data class ConfirmationExchangeRate(
    val rate: BigDecimal,
    val inputCurrency: Currency,
    val outputCurrency: Currency
)

private data class AnimatedConfirmationState(
    val confirmationExchangeRate: ConfirmationExchangeRate?,
    val minAmount: ConfirmationScreenExchangeAmount?,
    val networkFee: ConfirmationScreenExchangeAmount?,
    val bcdcFee: ConfirmationScreenExchangeAmount?
)
