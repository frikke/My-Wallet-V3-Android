package com.blockchain.core.chains.erc20.data.domain

import com.blockchain.api.ethereum.evm.BalancesResponse
import kotlinx.serialization.Serializable

@Serializable
internal data class Erc20L2BalancesStore(
    val balancesResponse: BalancesResponse,
    val accountHash: String
)

internal fun BalancesResponse.toStore(accountHash: String) = Erc20L2BalancesStore(
    balancesResponse = this,
    accountHash = accountHash
)

internal fun Erc20L2BalancesStore.toDomain() = balancesResponse
