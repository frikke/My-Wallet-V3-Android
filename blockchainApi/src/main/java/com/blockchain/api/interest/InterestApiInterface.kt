package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestAddressDto
import com.blockchain.api.interest.data.InterestAvailableTickersDto
import com.blockchain.api.interest.data.InterestEligibilityDto
import com.blockchain.api.interest.data.InterestRateDto
import com.blockchain.api.interest.data.InterestTickerLimitsDto
import com.blockchain.api.interest.data.InterestWithdrawalBodyDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

internal interface InterestApiInterface {
    @GET("accounts/savings")
    fun getAccountBalances(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<Response<Map<String, InterestAccountBalanceDto>>>

    @GET("savings/instruments")
    fun getAvailableTickersForInterest(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<InterestAvailableTickersDto>

    @GET("eligible/product/savings")
    fun getTickersEligibility(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<Map<String, InterestEligibilityDto>>

    @GET("savings/limits")
    fun getTickersLimits(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("currency") fiatCurrencyTicker: String
    ): Single<InterestTickerLimitsDto>

    @GET("savings/rates")
    fun getInterestRates(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<Response<InterestRateDto>>

    @GET("payments/accounts/savings")
    fun getAddress(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<InterestAddressDto>

    @POST("savings/withdrawals")
    fun performWithdrawal(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body body: InterestWithdrawalBodyDto
    ): Completable
}
