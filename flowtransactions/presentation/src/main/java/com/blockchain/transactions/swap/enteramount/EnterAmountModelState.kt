package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue

data class EnterAmountModelState(
    val walletMode: WalletMode? = null,

    val fromAccount: CryptoAccountWithBalance? = null,
    val toAccount: CryptoAccount? = null,
    val fiatCurrency: FiatCurrency,

    val config: DataResource<EnterAmountConfig> = DataResource.Loading,

    val fiatAmount: FiatValue? = null,
    val fiatAmountUserInput: String = "",
    val cryptoAmount: CryptoValue? = null,
    val cryptoAmountUserInput: String = "",

    val selectedInput: InputCurrency = InputCurrency.Currency1,

    val inputError: SwapEnterAmountInputError? = null,
    val fatalError: SwapEnterAmountFatalError? = null
) : ModelState

data class EnterAmountConfig(
    val sourceAccountToFiatRate: ExchangeRate,
    val limits: TxLimits,
)

sealed interface SwapEnterAmountInputError {
    data class BelowMinimum(val minValue: String) : SwapEnterAmountInputError
    data class AboveMaximum(val maxValue: String) : SwapEnterAmountInputError
    object AboveBalance : SwapEnterAmountInputError
}

sealed interface SwapEnterAmountFatalError {
    object WalletLoading : SwapEnterAmountFatalError
}
