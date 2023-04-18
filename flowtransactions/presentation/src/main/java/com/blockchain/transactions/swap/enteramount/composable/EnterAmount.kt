package com.blockchain.transactions.swap.enteramount.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.AlertButton
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallTertiaryButton
import com.blockchain.componentlib.card.TwoAssetActionHorizontal
import com.blockchain.componentlib.card.TwoAssetActionHorizontalLoading
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.componentlib.control.TwoCurrenciesInput
import com.blockchain.componentlib.control.isEmpty
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.enteramount.EnterAmountAssetState
import com.blockchain.transactions.swap.enteramount.EnterAmountAssets
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.EnterAmountViewState
import com.blockchain.transactions.swap.enteramount.SwapEnterAmountInputError
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterAmount(
    viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope),
    navControllerProvider: () -> NavHostController,
    onBackPressed: () -> Unit
) {
    val viewState: EnterAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(EnterAmountIntent.LoadData)
        onDispose { }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val newFrom = navControllerProvider()
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("your_key", null as? String?)
        ?.collectAsStateLifecycleAware()

    DisposableEffect(key1 = newFrom?.value) {
        newFrom?.value?.let {
            viewModel.onIntent(EnterAmountIntent.FromAccountChanged(it))
        }
        keyboardController?.show()
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.backgroundMuted)
    ) {
        NavigationBar(
            title = stringResource(R.string.common_swap),
            onBackButtonClick = onBackPressed,
        )

        EnterAmountScreen(
            selected = viewState.selectedInput,
            assets = viewState.assets,
            accountBalance = viewState.accountBalance,
            fiatAmount = viewState.fiatAmount,
            onFiatAmountChanged = {
                viewModel.onIntent(EnterAmountIntent.FiatInputChanged(it))
            },
            cryptoAmount = viewState.cryptoAmount,
            onCryptoAmountChanged = {
                viewModel.onIntent(EnterAmountIntent.CryptoInputChanged(it))
            },
            onFlipInputs = {
                viewModel.onIntent(EnterAmountIntent.FlipInputs)
            },
            error = viewState.error,
            openSourceAccounts = {
                openSourceAccounts(it)
                keyboardController?.hide()
            },
            setMaxOnClick = {
                viewModel.onIntent(EnterAmountIntent.MaxSelected)
            }
        )
    }
}

@Composable
private fun EnterAmountScreen(
    selected: InputCurrency,
    assets: DataResource<EnterAmountAssets>,
    accountBalance: String?,
    fiatAmount: CurrencyValue?,
    onFiatAmountChanged: (String) -> Unit,
    cryptoAmount: CurrencyValue?,
    onCryptoAmountChanged: (String) -> Unit,
    onFlipInputs: () -> Unit,
    error: SwapEnterAmountInputError?,
    setMaxOnClick: () -> Unit
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
                onCurrency1ValueChange = onFiatAmountChanged,
                currency2 = cryptoAmount,
                onCurrency2ValueChange = onCryptoAmountChanged,
                onFlipInputs = onFlipInputs,
            )
        }

        accountBalance?.let {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

            SmallTertiaryButton(
                modifier = Modifier.widthIn(min = 130.dp),
                text = stringResource(R.string.common_max_arg, accountBalance),
                onClick = setMaxOnClick,
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        when (assets) {
            DataResource.Loading -> {
                TwoAssetActionHorizontalLoading()
            }
            is DataResource.Data -> {
                TwoAssetActionHorizontal(
                    startTitle = "From",
                    startSubtitle = assets.data.from.ticker,
                    startIcon = StackedIcon.SingleIcon(ImageResource.Remote(assets.data.from.iconUrl)),
                    startOnClick = { openSourceAccounts(assets.data.from.ticker) },
                    endTitle = "To",
                    endSubtitle = assets.data.to.ticker,
                    endIcon = StackedIcon.SingleIcon(ImageResource.Remote(assets.data.to.iconUrl)),
                    endOnClick = {}
                )
            }
            is DataResource.Error -> TODO()
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        error?.let {
            AlertButton(
                modifier = Modifier.fillMaxWidth(),
                text = when (error) {
                    is SwapEnterAmountInputError.BelowMinimum -> {
                        stringResource(R.string.minimum_with_value, error.minValue)
                    }
                    is SwapEnterAmountInputError.AboveMaximum -> {
                        stringResource(R.string.maximum_with_value, error.maxValue)
                    }
                    is SwapEnterAmountInputError.AboveBalance -> {
                        stringResource(R.string.not_enough_funds, assets.map { it.from.ticker }.dataOrElse(""))
                    }
                },
                state = ButtonState.Disabled,
                onClick = {}
            )
        } ?: PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.preview_swap),
            state = if (fiatAmount?.isEmpty() == false && cryptoAmount?.isEmpty() == false) {
                ButtonState.Enabled
            } else {
                ButtonState.Disabled
            },
            onClick = {}
        )

        Spacer(modifier = Modifier.weight(3F))
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
private fun PreviewEnterAmountScreen() {
    EnterAmountScreen(
        selected = InputCurrency.Currency1,
        assets = DataResource.Data(
            EnterAmountAssets(
                from = EnterAmountAssetState(
                    iconUrl = "",
                    ticker = "BTC"
                ),
                to = EnterAmountAssetState(
                    iconUrl = "",
                    ticker = "ETH"
                )
            )
        ),
        accountBalance = "123.00",
        fiatAmount = CurrencyValue(
            value = "2,100.00", maxFractionDigits = 2, ticker = "$", isPrefix = true, separateWithSpace = false
        ),
        onFiatAmountChanged = {},
        cryptoAmount = CurrencyValue(
            value = "1.1292", maxFractionDigits = 8, ticker = "ETH", isPrefix = false, separateWithSpace = true
        ),
        onCryptoAmountChanged = {},
        onFlipInputs = {},
        error = SwapEnterAmountInputError.BelowMinimum("éjdzjjdz"),
        openSourceAccounts = {},
        setMaxOnClick = {},
    )
}
