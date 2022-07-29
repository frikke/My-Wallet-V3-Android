package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestAvailableTickersDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

private const val BALANCES = "accounts/savings"
private const val AVAILABLE_TICKERS = "savings/instruments"

internal interface InterestApiInterface {
    @GET(BALANCES)
    fun getAccountBalances(
        @Header("authorization") authorization: String
    ): Single<Response<Map<String, InterestAccountBalanceDto>>>

    @GET(AVAILABLE_TICKERS)
    fun getAvailableTickersForInterest(
        @Header("authorization") authorization: String
    ): Single<InterestAvailableTickersDto>
}
