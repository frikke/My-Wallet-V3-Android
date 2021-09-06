package com.blockchain.bitpay.models

data class BitPayPaymentResponse(val payment: BitPayVerification)

data class BitPayVerification(val chain: String, val transactions: List<BitPayTransaction>)