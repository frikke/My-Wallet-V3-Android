package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency

data class SwapEnterAmountViewState(
    val selectedInput: InputCurrency,
    val assets: EnterAmountAssets?,
    val maxAmount: String?,
    val fiatAmount: CurrencyValue?,
    val cryptoAmount: CurrencyValue?,
    val snackbarError: Exception?,
    val previewButtonState: PreviewButtonState,
    val fatalError: SwapEnterAmountFatalError?
) : ViewState

sealed interface PreviewButtonState {
    object Enabled : PreviewButtonState
    object Disabled : PreviewButtonState
    data class Error(val error: SwapEnterAmountInputError) : PreviewButtonState
}

data class EnterAmountAssets(
    val from: EnterAmountAssetState,
    val to: EnterAmountAssetState?
)

data class EnterAmountAssetState(
    val iconUrl: String,
    val nativeAssetIconUrl: String?,
    val ticker: String
)
