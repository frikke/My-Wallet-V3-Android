package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

internal interface InterestApiInterface {
    @GET("accounts/savings")
    fun getAllInterestAccountBalances(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<Response<Map<String, InterestAccountBalanceDto>>>
}
