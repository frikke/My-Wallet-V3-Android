package com.blockchain.bitpay.models

import kotlinx.serialization.Serializable

@Serializable
data class BitPayChain(
    val chain: String
)
