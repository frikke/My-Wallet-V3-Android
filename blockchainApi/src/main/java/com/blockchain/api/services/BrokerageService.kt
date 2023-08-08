package com.blockchain.api.services

import com.blockchain.api.brokerage.BrokerageApi
import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.QuoteRequestBody
import com.blockchain.outcome.Outcome

class BrokerageService internal constructor(private val api: BrokerageApi) {

    suspend fun fetchQuote(
        pair: String,
        inputValue: String,
        profile: String,
        paymentMethod: String,
        paymentMethodId: String?
    ): Outcome<Exception, BrokerageQuoteResponse> =
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
