package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.control.CurrencyValue

data class EnterAmountViewState(
    val fromAsset: EnterAmountAssetState?,
    val toAsset: EnterAmountAssetState?,
    val fiatAmount: CurrencyValue?,
    val cryptoAmount: CurrencyValue?,
) : ViewState

data class EnterAmountAssetState(
    val iconUrl: String,
    val ticker: String
)