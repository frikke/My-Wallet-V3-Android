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
import retrofit2.http.POST
import retrofit2.http.Query

internal interface InterestApiInterface {
    @GET("accounts/savings")
    fun getAccountBalances(): Single<Response<Map<String, InterestAccountBalanceDto>>>

    @GET("savings/instruments")
    fun getAvailableTickersForInterest(): Single<InterestAvailableTickersDto>

    @GET("earn/eligible")
    fun getTickersEligibility(
        @Query("product") product: String = "SAVINGS"
    ): Single<Map<String, InterestEligibilityDto>>

    @GET("savings/limits")
    fun getTickersLimits(
        @Query("currency") fiatCurrencyTicker: String
    ): Single<InterestTickerLimitsDto>

    @GET("savings/rates")
    fun getInterestRates(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<Response<InterestRateDto>>

    @GET("payments/accounts/savings")
    fun getAddress(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<InterestAddressDto>

    @POST("savings/withdrawals")
    fun performWithdrawal(
        @Body body: InterestWithdrawalBodyDto
    ): Completable
}
