package com.blockchain.transactions.sell.enteramount

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.CryptoAccount
import java.io.Serializable

data class SellEnterAmountArgs(
    val sourceAccount: Bindable<CryptoAccount?>
) : Serializable
