package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.control.CurrencyValue

data class EnterAmountModelState(
    val fromAccount: CryptoAccount? = null,
    val toAccount: CryptoAccount? = null,

    val fiatAmount: CurrencyValue? = null,
    val cryptoAmount: CurrencyValue? = null,
) : ModelState