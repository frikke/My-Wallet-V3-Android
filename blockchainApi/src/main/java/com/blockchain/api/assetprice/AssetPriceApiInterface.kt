package com.blockchain.api.assetprice

import com.blockchain.api.assetprice.data.AssetPriceDto
import com.blockchain.api.assetprice.data.AvailableSymbolsDto
import com.blockchain.api.assetprice.data.PriceRequestPairDto
import com.blockchain.api.assetprice.data.PriceResponseMapDto
import com.blockchain.network.interceptor.DoNotLogResponseBody
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface AssetPriceApiInterface {

    @GET("price/symbols")
    @DoNotLogResponseBody
    fun getAvailableSymbols(
        @Query("api_key") apiKey: String
    ): Single<AvailableSymbolsDto>

    // Get the current or at a specific time price index of multiple currency pairs
    @POST("price/index-multi")
    @DoNotLogResponseBody
    fun getCurrentPrices(
        @Body pairs: List<PriceRequestPairDto>,
        @Query("api_key") apiKey: String
    ): Single<PriceResponseMapDto>

    // Get the historic price at specific time price of multiple currency pairs
    @POST("price/index-multi")
    @DoNotLogResponseBody
    fun getHistoricPrices(
        @Body pairs: List<PriceRequestPairDto>,
        @Query("time") time: Long,
        @Query("api_key") apiKey: String
    ): Single<PriceResponseMapDto>

    // Get a series of historical price index which covers a range of time represents in specific scale
    @GET("price/index-series")
    @DoNotLogResponseBody
    fun getHistoricPriceSince(
        @Query("base") base: String,
        @Query("quote") quote: String,
        @Query("start") start: Long, // Epoch seconds
        @Query("scale") scale: Int,
        @Query("api_key") apiKey: String
    ): Single<List<AssetPriceDto>>
}
