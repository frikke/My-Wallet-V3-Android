package com.blockchain.api.custodial

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

internal interface CustodialBalanceApi {

    @GET("accounts/simplebuy")
    fun tradingBalanceForAllAssets(): Single<Map<String, TradingBalanceResponseDto>>
}
