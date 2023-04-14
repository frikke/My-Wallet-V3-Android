package com.dex.presentation.confirmation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.dex.presentation.R
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.dex.presentation.AmountFieldConfig
import com.dex.presentation.DexTxSubscribeScreen
import com.dex.presentation.SourceAndDestinationAmountFields
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.BigInteger
import org.koin.androidx.compose.getViewModel

@Composable
fun DexConfirmationScreen(
    onBackPressed: () -> Unit,
    viewModel: DexConfirmationViewModel = getViewModel(scope = payloadScope),
) {

    val lifecycleOwner = LocalLifecycleOwner.current

    DexTxSubscribeScreen(
        subscribe = { viewModel.onIntent(ConfirmationIntent.SubscribeForTxUpdates) },
        unsubscribe = { viewModel.onIntent(ConfirmationIntent.UnSubscribeToTxUpdates) }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                viewModel.onIntent(ConfirmationIntent.LoadTransactionData)
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
            SourceAndDestinationAmountFields(
                modifier = Modifier.padding(
                    top = AppTheme.dimensions.smallSpacing,
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                ),
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
            TransactionDetailsList(dataState)

            dataState.minAmount?.let { minAmount ->
                SimpleText(
                    modifier = Modifier.padding(
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing,
                        top = AppTheme.dimensions.standardSpacing
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

            SwapButton(
                modifier = Modifier.padding(
                    top = AppTheme.dimensions.standardSpacing
                ),
                onClick = {
/*
                    viewModel.onIntent(ConfirmationIntent.ConfirmSwap)
*/
                }
            )
        }
    }
}

@Preview
@Composable
private fun SwapButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(
            topStart = AppTheme.dimensions.smallSpacing,
            topEnd = AppTheme.dimensions.smallSpacing,
            bottomEnd = 0.dp,
            bottomStart = 0.dp,
        )
    ) {
        PrimaryButton(
            modifier = Modifier.padding(
                all = AppTheme.dimensions.smallSpacing,
            ),
            text = stringResource(id = R.string.common_swap),
            onClick = onClick
        )
    }
}

@Composable
private fun ColumnScope.TransactionDetailsList(dataState: ConfirmationScreenViewState.DataConfirmationViewState) {
    LazyColumn(
        modifier = Modifier
            .padding(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.standardSpacing
            )
            .fillMaxWidth()
            .weight(weight = 1f, fill = false)
            .clip(RoundedCornerShape(AppTheme.dimensions.mediumSpacing)),
    ) {

        safeLet(
            dataState.dexExchangeRate, dataState.inputCurrency, dataState.outputCurrency
        ) { rate, inputCurrency, outputCurrency ->
            item(
                content = {
                    ExchangeRateConfirmation(rate, inputCurrency, outputCurrency)
                    Divider(color = BackgroundMuted)
                }
            )
        }

        dataState.slippage?.let { sl ->
            item(
                content = {
                    SlippageConfirmation(sl)
                    Divider(color = BackgroundMuted)
                }
            )
        }
        dataState.minAmount?.let { minAmount ->
            item(
                content = {
                    MinAmountConfirmation(minAmount)
                    Divider(color = BackgroundMuted)
                }
            )
        }
        dataState.networkFee?.let { networkFee ->
            item(
                content = {
                    NetworkFee(networkFee)
                    Divider(color = BackgroundMuted)
                }
            )
        }
        dataState.blockchainFee?.let { blockFee ->
            item(
                content = {
                    BlockchainFee(blockFee)
                }
            )
        }
    }
}

@Composable
private fun NetworkFee(networkFee: ConfirmationScreenExchangeAmount) {
    TableRow(
        content = {
            SimpleText(
                modifier = Modifier.align(CenterVertically),
                text = stringResource(id = R.string.network_fee),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
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
    )
}

@Composable
private fun BlockchainFee(fee: ConfirmationScreenExchangeAmount) {
    TableRow(
        content = {
            SimpleText(
                modifier = Modifier.align(CenterVertically),
                text = stringResource(id = R.string.bcdc_fee),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
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
    )
}

@Composable
private fun MinAmountConfirmation(minAmount: ConfirmationScreenExchangeAmount) {
    TableRow(
        content = {
            SimpleText(
                modifier = Modifier.align(CenterVertically),
                text = stringResource(id = R.string.min_amount),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
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
                value = Money.fromMinor(CryptoCurrency.ETHER, BigInteger("2312312312")),
                exchange = Money.fromMinor(CryptoCurrency.ETHER, BigInteger("2312"))
            )
        )
    }
}

private fun Double.toPercentageString(): String {
    val percentage = this * 100.0
    return "%,.2f%%".format(percentage)
}
