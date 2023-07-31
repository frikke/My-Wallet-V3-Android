package com.blockchain.transactions.sell.confirmation

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import info.blockchain.balance.CryptoValue
import java.io.Serializable

data class SellConfirmationArgs(
    val sourceAccount: Bindable<CryptoAccount>,
    val targetAccount: Bindable<FiatAccount>,
    val sourceCryptoAmount: CryptoValue,
    val secondPassword: String?
) : Serializable
