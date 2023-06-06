package com.blockchain.transactions.sell.enteramount.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.betternavigation.NavContext
import com.blockchain.betternavigation.navigateTo
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.card.HorizontalAssetAction
import com.blockchain.componentlib.card.TwoAssetActionHorizontal
import com.blockchain.componentlib.card.TwoAssetActionHorizontalLoading
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.TwoCurrenciesInput
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Network
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.componentlib.keyboard.NumericKeyboard
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.extensions.safeLet
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import com.blockchain.presentation.complexcomponents.QuickFillDisplayAndAmount
import com.blockchain.presentation.complexcomponents.QuickFillRow
import com.blockchain.stringResources.R
import com.blockchain.transactions.sell.SellGraph
import com.blockchain.transactions.sell.confirmation.SellConfirmationArgs
import com.blockchain.transactions.sell.enteramount.EnterAmountAssetState
import com.blockchain.transactions.sell.enteramount.EnterAmountAssets
import com.blockchain.transactions.sell.enteramount.SellEnterAmountFatalError
import com.blockchain.transactions.sell.enteramount.SellEnterAmountInputError
import com.blockchain.transactions.sell.enteramount.SellEnterAmountIntent
import com.blockchain.transactions.sell.enteramount.SellEnterAmountNavigationEvent
import com.blockchain.transactions.sell.enteramount.SellEnterAmountViewModel
import com.blockchain.transactions.sell.enteramount.SellEnterAmountViewState
import com.blockchain.transactions.sell.targetassets.SellTargetAssetsArgs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.koin.androidx.compose.get

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavContext.SellEnterAmount(
    viewModel: SellEnterAmountViewModel,
    analytics: Analytics = get(),
    onBackPressed: () -> Unit
) {
    val viewState: SellEnterAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    val navigationEvent by viewModel.navigationEventFlow.collectAsStateLifecycleAware(null)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is SellEnterAmountNavigationEvent.TargetAssets -> {
                    navigateTo(
                        SellGraph.TargetAsset,
                        SellTargetAssetsArgs(
                            sourceAccount = Bindable(event.sourceAccount),
                            secondPassword = event.secondPassword
                        )
                    )
                }
                is SellEnterAmountNavigationEvent.Preview -> {
                    navigateTo(
                        SellGraph.Confirmation,
                        SellConfirmationArgs(
                            sourceAccount = Bindable(event.sourceAccount),
                            targetAccount = Bindable(event.targetAccount),
                            sourceCryptoAmount = event.sourceCryptoAmount,
                            secondPassword = event.secondPassword,
                        )
                    )
                }
            }
        }
    }

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(viewState.snackbarError) {
            val error = viewState.snackbarError
            if (error != null) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.localizedMessage ?: context.getString(R.string.common_error),
                    duration = SnackbarDuration.Short,
                )
                viewModel.onIntent(SellEnterAmountIntent.SnackbarErrorHandled)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(color = AppTheme.colors.background)
        ) {
            NavigationBar(
                title = stringResource(R.string.common_sell),
                onBackButtonClick = onBackPressed,
            )

            when (viewState.fatalError) {
                SellEnterAmountFatalError.WalletLoading -> {
                    CustomEmptyState(
                        icon = Icons.Network.id,
                        ctaAction = { }
                    )
                }
                null -> {
                    EnterAmountScreen(
                        selected = viewState.selectedInput,
                        assets = viewState.assets,
                        quickFillButtonData = viewState.quickFillButtonData,
                        fiatAmount = viewState.fiatAmount,
                        keyboardClicked = { button ->
                            viewModel.onIntent(SellEnterAmountIntent.KeyboardClicked(button))
                        },
                        cryptoAmount = viewState.cryptoAmount,
                        onFlipInputs = {
                            viewModel.onIntent(SellEnterAmountIntent.FlipInputs)
                        },
                        isConfirmEnabled = viewState.isConfirmEnabled,
                        inputError = viewState.inputError,
                        inputErrorClicked = { error ->
                            navigateTo(SellGraph.InputError, error)
                        },
                        openSourceAccounts = {
                            navigateTo(SellGraph.SourceAccounts)
                        },
                        quickFillEntryClicked = { entry ->
                            viewModel.onIntent(SellEnterAmountIntent.QuickFillEntryClicked(entry))
                        },
                        setMaxOnClick = {
                            viewModel.onIntent(SellEnterAmountIntent.MaxSelected)
                        },
                        previewClicked = {
                            viewModel.onIntent(SellEnterAmountIntent.PreviewClicked)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterAmountScreen(
    selected: InputCurrency,
    assets: EnterAmountAssets?,
    quickFillButtonData: QuickFillButtonData?,
    fiatAmount: CurrencyValue?,
    cryptoAmount: CurrencyValue?,
    keyboardClicked: (KeyboardButton) -> Unit,
    onFlipInputs: () -> Unit,
    isConfirmEnabled: Boolean,
    inputError: SellEnterAmountInputError?,
    inputErrorClicked: (SellEnterAmountInputError) -> Unit,
    openSourceAccounts: () -> Unit,
    quickFillEntryClicked: (QuickFillDisplayAndAmount) -> Unit,
    setMaxOnClick: () -> Unit,
    previewClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        safeLet(
            fiatAmount,
            cryptoAmount,
        ) { fiatAmount, cryptoAmount ->
            TwoCurrenciesInput(
                selected = selected,
                currency1 = fiatAmount,
                currency2 = cryptoAmount,
                onFlipInputs = onFlipInputs,
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        if (quickFillButtonData != null) {
            QuickFillRow(
                quickFillButtonData = quickFillButtonData,
                onQuickFillItemClick = quickFillEntryClicked,
                onMaxItemClick = { setMaxOnClick() },
                maxButtonText = stringResource(R.string.sell_enter_amount_max),
                areButtonsTransparent = false,
            )

            SmallVerticalSpacer()
        }

        assets?.let {
            TwoAssetActionHorizontal(
                startTitle = stringResource(R.string.common_sell),
                start = assets.from.toAssetAction(),
                startOnClick = openSourceAccounts,
                endTitle = stringResource(R.string.sell_enteramount_for),
                end = assets.to.toAssetAction(),
                endOnClick = null
            )
        } ?: TwoAssetActionHorizontalLoading()

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        if (inputError != null) {
            AlertButton(
                modifier = Modifier.fillMaxWidth(),
                text = when (inputError) {
                    is SellEnterAmountInputError.BelowMinimum -> {
                        stringResource(R.string.minimum_with_value, inputError.minValue)
                    }
                    is SellEnterAmountInputError.AboveMaximum -> {
                        stringResource(R.string.maximum_with_value, inputError.maxValue)
                    }
                    is SellEnterAmountInputError.AboveBalance -> {
                        stringResource(R.string.not_enough_funds, assets?.from?.ticker.orEmpty())
                    }
                    is SellEnterAmountInputError.InsufficientGas ->
                        stringResource(R.string.confirm_status_msg_insufficient_gas, inputError.displayTicker)
                    is SellEnterAmountInputError.Unknown ->
                        stringResource(R.string.common_error)
                },
                state = ButtonState.Enabled,
                onClick = { inputErrorClicked(inputError) }
            )
        } else {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.tx_enter_amount_sell_cta),
                state = if (isConfirmEnabled) {
                    ButtonState.Enabled
                } else {
                    ButtonState.Disabled
                },
                onClick = {
                    previewClicked()
                }
            )
        }

        SmallVerticalSpacer()

        NumericKeyboard(onClick = keyboardClicked)
    }
}

private fun EnterAmountAssetState.toAssetAction() = HorizontalAssetAction(
    assetName = ticker,
    icon = if (nativeAssetIconUrl != null) {
        StackedIcon.SmallTag(
            ImageResource.Remote(iconUrl),
            ImageResource.Remote(nativeAssetIconUrl)
        )
    } else {
        StackedIcon.SingleIcon(ImageResource.Remote(iconUrl))
    }
)

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
private fun PreviewEnterAmountScreen() {
    Box {
        EnterAmountScreen(
            selected = InputCurrency.Currency1,
            assets = EnterAmountAssets(
                from = EnterAmountAssetState(
                    iconUrl = "",
                    nativeAssetIconUrl = "",
                    ticker = "BTC",
                    name = "Bitcoin"
                ),
                to = EnterAmountAssetState(
                    iconUrl = "",
                    nativeAssetIconUrl = null,
                    ticker = "USD",
                    name = "US Dollars"
                )
            ),
            quickFillButtonData = QuickFillButtonData(
                quickFillButtons = listOf(
                    QuickFillDisplayAndAmount(
                        displayValue = "25%",
                        amount = CryptoValue.zero(CryptoCurrency.BTC),
                        roundingData = QuickFillRoundingData.SellSwapRoundingData(0.25f, emptyList()),
                        position = 0
                    ),
                    QuickFillDisplayAndAmount(
                        displayValue = "50%",
                        amount = CryptoValue.zero(CryptoCurrency.BTC),
                        roundingData = QuickFillRoundingData.SellSwapRoundingData(0.5f, emptyList()),
                        position = 1
                    ),
                    QuickFillDisplayAndAmount(
                        displayValue = "75%",
                        amount = CryptoValue.zero(CryptoCurrency.BTC),
                        roundingData = QuickFillRoundingData.SellSwapRoundingData(0.75f, emptyList()),
                        position = 2
                    ),
                ),
                maxAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 1.1234567890123457.toBigDecimal())
            ),
            fiatAmount = CurrencyValue(
                value = "2,100.00",
                maxFractionDigits = 2,
                ticker = "$",
                isPrefix = true,
                separateWithSpace = false,
                zeroHint = "0"

            ),
            keyboardClicked = {},
            cryptoAmount = CurrencyValue(
                value = "1.1292",
                maxFractionDigits = 8,
                ticker = "ETH",
                isPrefix = false,
                separateWithSpace = true,
                zeroHint = "0"

            ),
            onFlipInputs = {},
            isConfirmEnabled = false,
            inputError = SellEnterAmountInputError.BelowMinimum("éjdzjjdz"),
            inputErrorClicked = {},
            openSourceAccounts = {},
            quickFillEntryClicked = {},
            setMaxOnClick = {},
            previewClicked = {},
        )
    }
}
