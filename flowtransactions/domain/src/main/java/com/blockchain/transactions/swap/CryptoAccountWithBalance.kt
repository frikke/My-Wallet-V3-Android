package com.blockchain.transactions.swap

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.Money

data class CryptoAccountWithBalance(
    val account: CryptoAccount,
    val balanceCrypto: Money,
    val balanceFiat: Money
)
