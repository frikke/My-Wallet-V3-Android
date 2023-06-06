package com.blockchain.transactions.swap.enteramount

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.CryptoAccount
import java.io.Serializable

data class SwapEnterAmountArgs(
    val sourceAccount: Bindable<CryptoAccount?>
) : Serializable
