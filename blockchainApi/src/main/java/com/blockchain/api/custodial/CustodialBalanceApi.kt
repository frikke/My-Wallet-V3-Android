package com.blockchain.api.custodial

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header

internal interface CustodialBalanceApi {

    @GET("accounts/simplebuy")
    fun tradingBalanceForAllAssets(
        @Header("authorization") authorization: String
    ): Single<Map<String, TradingBalanceResponseDto>>
}