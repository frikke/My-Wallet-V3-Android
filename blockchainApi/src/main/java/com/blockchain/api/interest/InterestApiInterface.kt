package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

internal interface InterestApiInterface {
    @GET("accounts/savings")
    fun getAccountBalances(
        @Header("authorization") authorization: String
    ): Single<Response<Map<String, InterestAccountBalanceDto>>>
}
