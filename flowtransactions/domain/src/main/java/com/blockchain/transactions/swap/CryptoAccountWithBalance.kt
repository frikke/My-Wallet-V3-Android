package com.blockchain.transactions.swap

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.Money
import java.io.Serializable

data class CryptoAccountWithBalance(
    val account: CryptoAccount,
    val balanceCrypto: Money,
    val balanceFiat: Money
) : Serializable
