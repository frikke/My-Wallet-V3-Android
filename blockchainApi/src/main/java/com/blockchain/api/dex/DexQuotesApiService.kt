package com.blockchain.api.dex

import com.blockchain.outcome.Outcome

class DexQuotesApiService(private val dexQuotesApi: DexQuotesApi) {
    suspend fun quote(
        fromCurrency: FromCurrency,
        toCurrency: ToCurrency,
        slippage: Double,
        address: String
    ): Outcome<Exception, DexQuoteResponse> =
        dexQuotesApi.quote(
            product = DEX_PRODUCT,
            request = DexQuoteRequest(
                venue = DEFAULT_VENUE,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                takerAddress = address,
                params = DexQuotesParams(
                    slippage = slippage.toString()
                )
            )
        )
}

private const val DEFAULT_VENUE = "ZEROX"
private const val DEX_PRODUCT = "DEX"

@kotlinx.serialization.Serializable
data class DexQuoteResponse(
    val type: String,
    val quote: QuoteResponse
)

@kotlinx.serialization.Serializable
data class QuoteResponse(
    val buyAmount: DexQuoteAmount,
    val sellAmount: DexQuoteAmount
)

@kotlinx.serialization.Serializable
data class DexQuoteAmount(
    val amount: String,
    val minAmount: String?
)

@kotlinx.serialization.Serializable
data class DexQuoteRequest(
    private val venue: String,
    private val fromCurrency: FromCurrency,
    private val toCurrency: ToCurrency,
    private val takerAddress: String,
    private val params: DexQuotesParams
)

@kotlinx.serialization.Serializable
data class FromCurrency(
    private val chainId: Int,
    private val symbol: String,
    private val address: String,
    private val amount: String,
)

@kotlinx.serialization.Serializable
data class ToCurrency(
    private val chainId: Int,
    private val symbol: String,
    private val address: String
)

@kotlinx.serialization.Serializable
data class DexQuotesParams(
    private val slippage: String
)
