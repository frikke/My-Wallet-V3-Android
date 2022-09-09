package com.blockchain.api.services

import com.blockchain.api.brokerage.BrokerageApi
import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.QuoteRequestBody
import io.reactivex.rxjava3.core.Single

class BrokerageService internal constructor(private val api: BrokerageApi) {

    fun fetchQuote(
        pair: String,
        inputValue: String,
        profile: String,
        paymentMethod: String,
        paymentMethodId: String?
    ): Single<BrokerageQuoteResponse> =
        api.fetchQuote(
            quoteRequestBody = QuoteRequestBody(
                inputValue = inputValue,
                profile = profile,
                paymentMethod = paymentMethod,
                paymentMethodId = paymentMethodId,
                pair = pair
            )
        )
}
