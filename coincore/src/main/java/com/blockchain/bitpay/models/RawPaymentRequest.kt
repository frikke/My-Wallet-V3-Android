package com.blockchain.bitpay.models

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RawPaymentRequest(
    val instructions: List<BitPaymentInstructions>,
    val memo: String,
    val expires: String,
    val paymentUrl: String
)

@Serializable
data class BitPaymentInstructions(
    val outputs: List<BitPayPaymentRequestOutput>
)

@Serializable
data class BitPayPaymentRequestOutput(
    val amount: @Contextual BigInteger,
    val address: String
)
