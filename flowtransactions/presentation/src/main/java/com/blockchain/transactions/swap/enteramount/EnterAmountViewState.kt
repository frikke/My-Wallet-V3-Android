package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency

data class EnterAmountViewState(
    val selectedInput: InputCurrency,
    val fromAsset: EnterAmountAssetState?,
    val toAsset: EnterAmountAssetState?,
    val fiatAmount: CurrencyValue?,
    val cryptoAmount: CurrencyValue?,
    val error: SwapEnterAmountInputError?,
) : ViewState

data class EnterAmountAssetState(
    val iconUrl: String,
    val ticker: String
)
