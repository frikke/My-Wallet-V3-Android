package com.blockchain.api.txlimits

import com.blockchain.api.txlimits.data.GetFeatureLimitsResponse
import com.blockchain.api.txlimits.data.GetSeamlessLimitsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TxLimitsApi {

    @GET("limits/crossborder/transaction")
    fun getSeamlessLimits(
        @Header("authorization") authorization: String,
        @Query("currency")
        outputCurrency: String,
        @Query("inputCurrency")
        sourceCurrency: String,
        @Query("outputCurrency")
        targetCurrency: String,
        @Query("fromAccount")
        sourceAccountType: String,
        @Query("toAccount")
        targetAccountType: String
    ): Single<GetSeamlessLimitsResponse>

    @GET("limits/overview")
    fun getFeatureLimits(
        @Header("authorization") authorization: String
    ): Single<GetFeatureLimitsResponse>
}
