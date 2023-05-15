package com.blockchain.transactions.swap

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.io.Serializable

data class CryptoAccountWithBalance(
    val account: CryptoAccount,
    val balanceCrypto: CryptoValue,
    val balanceFiat: FiatValue,
) : Serializable
