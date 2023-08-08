package com.blockchain.api.dex

import com.blockchain.outcome.Outcome
import kotlinx.serialization.SerialName

class DexQuotesApiService(private val dexQuotesApi: DexQuotesApi) {
    suspend fun quote(
        fromCurrency: FromCurrency,
        toCurrency: ToCurrency,
        slippage: Double,
        address: String,
        skipValidation: Boolean
    ): Outcome<Exception, DexQuoteResponse> =
        dexQuotesApi.quote(
            product = DEX_PRODUCT,
            request = DexQuoteRequest(
                venue = DEFAULT_VENUE,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                takerAddress = address,
                params = DexQuotesParams(
                    slippage = slippage.toString(),
                    skipValidation = skipValidation
                )
            )
        )
}

private const val DEFAULT_VENUE = "ZEROX"
private const val DEX_PRODUCT = "DEX"

@kotlinx.serialization.Serializable
data class DexQuoteResponse(
    val type: String,
    val quote: QuoteResponse,
    @SerialName("tx")
    val transaction: DexTransactionResponse,
    val quoteTtl: Long
)

@kotlinx.serialization.Serializable
data class QuoteResponse(
    val buyAmount: DexQuoteAmount,
    val sellAmount: DexQuoteAmount,
    val price: String,
    val buyTokenFee: String
)

@kotlinx.serialization.Serializable
data class DexTransactionResponse(
    val chainId: Int,
    val to: String,
    val data: String,
    val value: String,
    val gasLimit: String, // In Gas units
    val gasPrice: String // In WEI
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
    private val amount: String?
)

@kotlinx.serialization.Serializable
data class ToCurrency(
    private val chainId: Int,
    private val symbol: String,
    private val address: String,
    private val amount: String?
)

@kotlinx.serialization.Serializable
data class DexQuotesParams(
    private val slippage: String,
    private val skipValidation: Boolean
)
