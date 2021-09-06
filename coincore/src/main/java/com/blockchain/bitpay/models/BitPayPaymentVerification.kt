package com.blockchain.bitpay.models

data class BitPaymentRequest(val chain: String, val transactions: List<BitPayTransaction>)
data class BitPayTransaction(val tx: String, val weightedSize: Int)