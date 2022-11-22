package com.blockchain.domain.wallet

import kotlinx.serialization.Serializable

@Serializable
data class CoinType(
    val network: NetworkType,
    val type: Int,
    val purpose: Int
)
