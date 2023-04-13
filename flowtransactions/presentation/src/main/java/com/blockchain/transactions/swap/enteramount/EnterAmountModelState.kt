package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.control.CurrencyValue
import com.blockchain.componentlib.control.InputCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal

data class EnterAmountModelState(
    val fromAccount: CryptoAccount? = null,
    val toAccount: CryptoAccount? = null,

    val exchangeRate: ExchangeRate? = null,

    val fiatCurrency: FiatCurrency? = null,
    val fiatAmount: Money? = null,
    val fiatAmountUserInput: String = "",
    val cryptoAmount: Money? = null,
    val cryptoAmountUserInput: String = "",

    val selectedInput: InputCurrency = InputCurrency.Currency1
) : ModelState