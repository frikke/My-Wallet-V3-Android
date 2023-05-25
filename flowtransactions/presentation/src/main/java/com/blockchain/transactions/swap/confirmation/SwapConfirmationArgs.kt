package com.blockchain.transactions.swap.confirmation

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.CryptoValue
import java.io.Serializable

data class SwapConfirmationArgs(
    val sourceAccount: Bindable<CryptoAccount>,
    val targetAccount: Bindable<CryptoAccount>,
    val sourceCryptoAmount: CryptoValue,
    val secondPassword: String?
) : Serializable
