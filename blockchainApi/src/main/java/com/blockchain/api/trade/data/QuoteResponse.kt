package com.blockchain.api.trade.data

import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponse(
    val currencyPair: String,
    val amount: String, // source curr
    val price: String, // price, in target curr, for each "major unit" of source curr
    val resultAmount: String, // (amount(major)-dynamicFee)*price - networkFee
    val dynamicFee: String, // source curr
    // networkFee refers to the 2nd leg of the transaction, to in case of NC BTC -> NC ETH,
    // it will only refer to the NC ETH networkFee, we'll still have to calculate the NC BTC networkFee ourselves
    val networkFee: String?, // destination curr
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
