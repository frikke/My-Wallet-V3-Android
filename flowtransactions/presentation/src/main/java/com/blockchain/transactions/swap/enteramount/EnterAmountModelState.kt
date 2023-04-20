package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.control.InputCurrency
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

data class EnterAmountModelState(
    val accounts: DataResource<EnterAmountAccounts> = DataResource.Loading,
    val config: DataResource<EnterAmountConfig> = DataResource.Loading,

    val fiatAmount: Money? = null,
    val fiatAmountUserInput: String = "",
    val cryptoAmount: Money? = null,
    val cryptoAmountUserInput: String = "",

    val selectedInput: InputCurrency = InputCurrency.Currency1,

    val inputError: SwapEnterAmountInputError? = null,
    val fatalError: SwapEnterAmountFatalError? = null
) : ModelState

data class EnterAmountAccounts(
    // for the FROM account we need to keep reference to the balance to cap the input
    val fromAccount: CryptoAccountWithBalance,
    val toAccount: CryptoAccount?,
    val fiatCurrency: FiatCurrency,
)

data class EnterAmountConfig(
    val sourceAccountToFiat: ExchangeRate,
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
