package com.blockchain.core.chains.erc20.data.domain

import com.blockchain.api.services.Erc20TokenBalance
import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class Erc20TokenBalancesStore(
    val erc20TokenBalances: List<Erc20TokenBalanceStore>,
    val accountHash: String
)

@Serializable
internal data class Erc20TokenBalanceStore(
    val ticker: String,
    val contractAddress: String,
    val balance: @Contextual BigInteger,
    val precisionDp: Int,
    val transferCount: Int
)

internal fun List<Erc20TokenBalance>.toStore(accountHash: String) = Erc20TokenBalancesStore(
    erc20TokenBalances = map { it.toStore() },
    accountHash = accountHash
)

internal fun Erc20TokenBalance.toStore() = Erc20TokenBalanceStore(
    ticker = ticker,
    contractAddress = contractAddress,
    balance = balance,
    precisionDp = precisionDp,
    transferCount = transferCount
)

internal fun Erc20TokenBalancesStore.toDomain(): List<Erc20TokenBalance> =
    erc20TokenBalances.map { it.toDomain() }

internal fun Erc20TokenBalanceStore.toDomain() = Erc20TokenBalance(
    ticker = ticker,
    contractAddress = contractAddress,
    balance = balance,
    precisionDp = precisionDp,
    transferCount = transferCount
)
