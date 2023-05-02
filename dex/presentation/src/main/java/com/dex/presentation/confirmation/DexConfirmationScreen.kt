package com.dex.presentation.confirmation

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
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
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
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Orange500
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.dex.presentation.AmountFieldConfig
import com.dex.presentation.DexTxSubscribeScreen
import com.dex.presentation.SourceAndDestinationAmountFields
import com.dex.presentation.graph.ARG_INFO_DESCRIPTION
import com.dex.presentation.graph.ARG_INFO_TITLE
import com.dex.presentation.graph.DexDestination
import com.dex.presentation.uierrors.AlertError
import com.dex.presentation.uierrors.DexUiError
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun DexConfirmationScreen(
    onBackPressed: () -> Unit,
    navController: NavController,
    viewModel: DexConfirmationViewModel = getViewModel(scope = payloadScope),
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
                    route = DexDestination.InProgress.route,
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

    Column(modifier = Modifier.fillMaxSize()) {
        NavigationBar(
            title = stringResource(R.string.confirm_swap),
            onBackButtonClick = onBackPressed,
        )
        val viewState: ConfirmationScreenViewState by viewModel.viewState.collectAsStateLifecycleAware()

        (viewState as? ConfirmationScreenViewState.DataConfirmationViewState)?.let { dataState ->
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
                            bottom = AppTheme.dimensions.smallestSpacing,
                        )
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        SourceAndDestinationAmountFields(
                            onValueChanged = { },
                            sourceAmountFieldConfig = AmountFieldConfig(
                                isReadOnly = true,
                                isEnabled = true,
                                exchange = dataState.exchangeInputAmount,
                                currency = dataState.inputCurrency,
                                max = null,
                                canChangeCurrency = false,
                                onCurrencyClicked = { },
                                amount = dataState.inputAmount,
                                balance = dataState.inputBalance,
                            ),

                            destinationAmountFieldConfig = AmountFieldConfig(
                                isReadOnly = true,
                                isEnabled = true,
                                canChangeCurrency = false,
                                exchange = dataState.outputExchangeAmount,
                                currency = dataState.outputCurrency,
                                max = null,
                                onCurrencyClicked = { },
                                amount = dataState.outputAmount,
                                balance = dataState.outputBalance,
                            )
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))
                    }
                    safeLet(
                        dataState.dexExchangeRate, dataState.inputCurrency, dataState.outputCurrency
                    ) { rate, inputCurrency, outputCurrency ->
                        item {
                            Card(
                                backgroundColor = AppTheme.colors.background,
                                shape = RoundedCornerShape(
                                    topStart = AppTheme.dimensions.mediumSpacing,
                                    topEnd = AppTheme.dimensions.mediumSpacing
                                ),
                                modifier = Modifier.padding(bottom = 1.dp),
                                elevation = 0.dp
                            ) {
                                ExchangeRateConfirmation(rate, inputCurrency, outputCurrency)
                                Divider(color = BackgroundMuted)
                            }
                        }
                    }
                    dataState.slippage?.let { sl ->
                        item {
                            SlippageConfirmation(sl)
                            Divider(color = BackgroundMuted)
                        }
                    }
                    dataState.minAmount?.let { minAmount ->
                        item {
                            MinAmountConfirmation(
                                minAmount = minAmount,
                                slippage = dataState.slippage ?: 0.0,
                                extraInfoOnClick = { destination ->
                                    navController.navigate(
                                        destination
                                    )
                                }
                            )
                            Divider(color = BackgroundMuted)
                        }
                    }
                    dataState.networkFee?.let { networkFee ->
                        item {
                            NetworkFee(
                                networkFee = networkFee,
                                extraInfoOnClick = { destination ->
                                    navController.navigate(
                                        destination
                                    )
                                }
                            )
                            Divider(color = BackgroundMuted)
                        }
                    }
                    dataState.blockchainFee?.let { blockFee ->
                        item {
                            Card(
                                backgroundColor = AppTheme.colors.background,
                                shape = RoundedCornerShape(
                                    bottomEnd = AppTheme.dimensions.mediumSpacing,
                                    bottomStart = AppTheme.dimensions.mediumSpacing
                                ),
                                elevation = 0.dp
                            ) {
                                BlockchainFee(
                                    fee = blockFee,
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
                                    bottom = AppTheme.dimensions.smallSpacing,
                                ),
                                text = stringResource(
                                    id = R.string.min_amount_estimation,
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
                    },
                    accept = {
                        viewModel.onIntent(ConfirmationIntent.AcceptPrice)
                    },
                    state = when {
                        dataState.operationInProgress -> ButtonState.Loading
                        dataState.newPriceAvailable -> ButtonState.Disabled
                        dataState.error != DexUiError.None -> ButtonState.Disabled
                        else -> ButtonState.Enabled
                    },
                    alertMessage = (dataState.error as? AlertError)?.message(LocalContext.current)
                )
            }
        }
    }
}

@Composable
private fun ConfirmationPinnedBottom(
    newPriceAvailableAndNotAccepted: Boolean,
    confirm: () -> Unit,
    accept: () -> Unit,
    state: ButtonState,
    alertMessage: String?,
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(
                    topStart = AppTheme.dimensions.smallSpacing,
                    topEnd = AppTheme.dimensions.smallSpacing,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp,
                )
            )
            .padding(
                all = AppTheme.dimensions.smallSpacing,
            )

    ) {
        if (newPriceAvailableAndNotAccepted) {
            PriceUpdateWarning(accept)
        }

        if (alertMessage != null) {
            AlertButton(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.small_spacing))
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
                color = Grey000,
                shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)
            )
            .padding(all = AppTheme.dimensions.smallSpacing),
        verticalAlignment = CenterVertically
    ) {
        Image(
            imageResource = Icons.Error.withTint(Orange500)
                .withSize(AppTheme.dimensions.standardSpacing)
        )
        SimpleText(
            modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing),
            text = stringResource(id = R.string.price_updated),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(id = R.string.accept),
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
        modifier = Modifier
            .fillMaxWidth(),
        state = state,
        text = stringResource(id = R.string.common_swap),
        onClick = onClick
    )
}

@Composable
private fun NetworkFee(networkFee: ConfirmationScreenExchangeAmount, extraInfoOnClick: (String) -> Unit) {
    val extraInfoTitle = stringResource(id = R.string.network_fee)
    val extraInfoDescription =
        stringResource(id = R.string.network_fee_info_description, networkFee.value.currency.displayTicker)
    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = R.string.network_fee),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = extraInfoDestination(
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
                    SimpleText(
                        text = networkFee.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    SimpleText(
                        text = "~ ${networkFee.exchange.toStringWithSymbol()}",
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
    val extraInfoTitle = stringResource(id = R.string.bcdc_fee)
    val extraInfoDescription = stringResource(id = R.string.bcdc_fee_extra_info_description)
    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = R.string.bcdc_fee),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = extraInfoDestination(
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
                    SimpleText(
                        text = fee.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    SimpleText(
                        text = "~ ${fee.exchange.toStringWithSymbol()}",
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
    val extraInfoTitle = stringResource(id = R.string.minimum_amount)
    val extraInfoDescription = stringResource(id = R.string.minimum_amount_extra_info, slippage.toPercentageString())

    TableRow(
        content = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically) {
                Row(verticalAlignment = CenterVertically) {
                    SimpleText(
                        modifier = Modifier.align(CenterVertically),
                        text = stringResource(id = R.string.min_amount),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Start
                    )

                    ExtraInfoIndicator {
                        val destination = extraInfoDestination(
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
                    SimpleText(
                        text = minAmount.value.toStringWithSymbol(),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.End
                    )
                    SimpleText(
                        text = "~ ${minAmount.exchange.toStringWithSymbol()}",
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
        imageResource = Icons.Question.withTint(Grey400).withSize(14.dp)
    )
}

private fun extraInfoDestination(title: String, description: String): String {
    return DexDestination.DexConfirmationExtraInfoSheet.routeWithArgs(
        listOf(
            NavArgument(
                key = ARG_INFO_TITLE,
                value = title
            ),
            NavArgument(
                key = ARG_INFO_DESCRIPTION,
                value = Base64.getUrlEncoder().encodeToString(
                    description.toByteArray()
                )
            ),
        ),
    )
}

@Composable
private fun ExchangeRateConfirmation(exchangeRate: BigDecimal, inputCurrency: AssetInfo, outputCurrency: AssetInfo) {
    TableRow(
        content = {
            SimpleText(
                text = stringResource(id = R.string.exchange_rate),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
            Spacer(modifier = Modifier.weight(1f))
            SimpleText(
                text = "$exchangeRate ${outputCurrency.displayTicker} / ${
                Money.fromMajor(
                    inputCurrency, BigDecimal.ONE
                ).toStringWithSymbol(includeDecimalsWhenWhole = false)
                }",
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.End
            )
        }
    )
}

@Composable
private fun SlippageConfirmation(sl: Double) {
    TableRow(
        content = {
            SimpleText(
                text = stringResource(id = R.string.allowed_slippage),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
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

private fun Double.toPercentageString(): String {
    val percentage = this * 100.0
    return "%,.2f%%".format(percentage)
}
