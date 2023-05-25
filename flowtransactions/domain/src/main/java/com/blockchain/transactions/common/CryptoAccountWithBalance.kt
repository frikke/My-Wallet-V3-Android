package com.blockchain.transactions.common

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

data class CryptoAccountWithBalance(
    val account: CryptoAccount,
    val balanceCrypto: CryptoValue,
    val balanceFiat: FiatValue,
)
