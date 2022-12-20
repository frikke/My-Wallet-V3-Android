package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponse(
    val currencyPair: String,
    val amount: String,
    val price: String,
    val resultAmount: String,
    val dynamicFee: String,
    val networkFee: String?,
    val paymentMethod: String,
    val orderProfileName: String,
) {
    companion object {
        const val FUNDS = "FUNDS" // default
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val BANK_TRANSFER = "BANK_TRANSFER"
        const val BANK_ACCOUNT = "BANK_ACCOUNT"

        const val SIMPLEBUY = "SIMPLEBUY" // default
        const val SIMPLETRADE = "SIMPLETRADE"
        const val SWAP_FROM_USERKEY = "SWAP_FROM_USERKEY"
        const val SWAP_ON_CHAIN = "SWAP_ON_CHAIN"
        const val RB_SIMPLEBUY = "RB_SIMPLEBUY"
    }
}
