package com.blockchain.bitpay.models

import kotlinx.serialization.Serializable

@Serializable
data class BitPaymentRequest(
    val chain: String,
    val transactions: List<BitPayTransaction>
)

@Serializable
data class BitPayTransaction(
    val tx: String,
    val weightedSize: Int
)
