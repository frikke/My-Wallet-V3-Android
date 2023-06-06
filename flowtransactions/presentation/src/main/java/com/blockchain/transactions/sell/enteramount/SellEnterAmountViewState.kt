package com.blockchain.transactions.sell.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.presentation.complexcomponents.QuickFillButtonData

data class SellEnterAmountViewState(
    val selectedInput: InputCurrency,
    val assets: EnterAmountAssets?,
    val quickFillButtonData: QuickFillButtonData?,
    val fiatAmount: CurrencyValue?,
    val cryptoAmount: CurrencyValue?,
    val snackbarError: Exception?,
    val inputError: SellEnterAmountInputError?,
    val fatalError: SellEnterAmountFatalError?,
    val isConfirmEnabled: Boolean,
) : ViewState

data class EnterAmountAssets(
    val from: EnterAmountAssetState,
    val to: EnterAmountAssetState,
)

data class EnterAmountAssetState(
    val iconUrl: String,
    val nativeAssetIconUrl: String?,
    val ticker: String,
    val name: String
)
