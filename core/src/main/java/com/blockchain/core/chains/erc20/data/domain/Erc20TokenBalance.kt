package com.blockchain.core.chains.erc20.data.domain

import com.blockchain.api.services.Erc20TokenBalance
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class Erc20TokenBalanceStore(
    val ticker: String,
    val contractAddress: String,
    val balance: @Contextual BigInteger,
    val precisionDp: Int,
    val transferCount: Int
)

fun Erc20TokenBalance.toStore() = Erc20TokenBalanceStore(
    ticker = ticker,
    contractAddress = contractAddress,
    balance = balance,
    precisionDp = precisionDp,
    transferCount = transferCount
)

fun Erc20TokenBalanceStore.toDomain() = Erc20TokenBalance(
    ticker = ticker,
    contractAddress = contractAddress,
    balance = balance,
    precisionDp = precisionDp,
    transferCount = transferCount
)