package com.blockchain.domain.wallet

import info.blockchain.balance.NetworkType
import kotlinx.serialization.Serializable

@Serializable
data class CoinType(
    val network: NetworkType,
    val type: Int,
    val purpose: Int
)
