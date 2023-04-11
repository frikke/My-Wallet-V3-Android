package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAsset
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.control.CurrencyValue

data class EnterAmountModelState(
    val fromAsset: CryptoAsset? = null, // should be account not asset
    val toAsset: CryptoAsset? = null, // should be account not asset

    val fiatAmount: CurrencyValue? = null,
    val cryptoAmount: CurrencyValue? = null,
) : ModelState