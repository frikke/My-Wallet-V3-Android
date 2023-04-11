package com.blockchain.transactions.swap.enteramount.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.card.TwoAssetAction
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.TwoCurrenciesInput
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.extensions.safeLet
import com.blockchain.koin.payloadScope
import com.blockchain.transactions.presentation.R
import com.blockchain.transactions.swap.enteramount.EnterAmountAssetState
import com.blockchain.transactions.swap.enteramount.EnterAmountIntent
import com.blockchain.transactions.swap.enteramount.EnterAmountViewModel
import com.blockchain.transactions.swap.enteramount.EnterAmountViewState
import org.koin.androidx.compose.getViewModel

@Composable
fun EnterAmount(
    viewModel: EnterAmountViewModel = getViewModel(scope = payloadScope),
    onBackPressed: () -> Unit
) {
    val viewState: EnterAmountViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(EnterAmountIntent.LoadData)
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

        safeLet(
            viewState.fromAsset,
            viewState.toAsset,
            viewState.fiatAmount,
            viewState.cryptoAmount,
        ) { fromAsset, toAsset, fiatAmount, cryptoAmount ->
            EnterAmountScreen(
                from = fromAsset,
                to = toAsset,
                fiatAmount = fiatAmount,
                onFiatAmountChanged = {
                    viewModel.onIntent(EnterAmountIntent.FiatAmountChanged(amount = it))

                },
                cryptoAmount = cryptoAmount,
                onCryptoAmountChanged = {
                    viewModel.onIntent(EnterAmountIntent.CryptoAmountChanged(amount = it))
                }
            )
        }
    }
}

@Composable
private fun EnterAmountScreen(
    from: EnterAmountAssetState,
    to: EnterAmountAssetState,
    fiatAmount: CurrencyValue,
    onFiatAmountChanged: (String) -> Unit,
    cryptoAmount: CurrencyValue,
    onCryptoAmountChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1F))

        TwoCurrenciesInput(
            currency1 = fiatAmount,
            onCurrency1ValueChange = onFiatAmountChanged,
            currency2 = cryptoAmount,
            onCurrency2ValueChange = onCryptoAmountChanged
        )

        Spacer(modifier = Modifier.weight(1F))

        TwoAssetAction(
            startTitle = "From",
            startSubtitle = from.ticker,
            startIcon = StackedIcon.SingleIcon(ImageResource.Remote(from.iconUrl)),
            endTitle = "To",
            endSubtitle = to.ticker,
            endIcon = StackedIcon.SingleIcon(ImageResource.Remote(to.iconUrl)),
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.preview_swap),
            onClick = {}
        )

        Spacer(modifier = Modifier.weight(4F))
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF0F2F7)
@Composable
private fun PreviewEnterAmountScreen() {
    EnterAmountScreen(
        from = EnterAmountAssetState(
            iconUrl = "",
            ticker = "BTC"
        ),
        to = EnterAmountAssetState(
            iconUrl = "",
            ticker = "ETH"
        ),
        fiatAmount = CurrencyValue(
            value = "2,100.00", ticker = "$", isPrefix = true, separateWithSpace = false
        ),
        onFiatAmountChanged = {},
        cryptoAmount = CurrencyValue(
            value = "1.1292", ticker = "ETH", isPrefix = false, separateWithSpace = true
        ),
        onCryptoAmountChanged = {}
    )
}