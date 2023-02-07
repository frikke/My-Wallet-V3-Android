package com.blockchain.api.earn.passive

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.passive.data.InterestAccountBalanceDto
import com.blockchain.api.earn.passive.data.InterestAddressDto
import com.blockchain.api.earn.passive.data.InterestAvailableTickersDto
import com.blockchain.api.earn.passive.data.InterestRateDto
import com.blockchain.api.earn.passive.data.InterestRatesDto
import com.blockchain.api.earn.passive.data.InterestTickerLimitsDto
import com.blockchain.api.earn.passive.data.InterestWithdrawalBodyDto
import com.blockchain.outcome.Outcome
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
    ): Single<Map<String, EarnRewardsEligibilityDto>>

    @GET("savings/limits")
    fun getTickersLimits(
        @Query("currency") fiatCurrencyTicker: String
    ): Single<InterestTickerLimitsDto>

    @GET("savings/rates")
    fun getInterestRates(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<Response<InterestRateDto>>

    @GET("earn/rates-user")
    suspend fun getAllInterestRates(
        @Query("product") product: String = "SAVINGS"
    ): Outcome<Exception, InterestRatesDto>

    @GET("payments/accounts/savings")
    fun getAddress(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<InterestAddressDto>

    @POST("savings/withdrawals")
    fun performWithdrawal(
        @Body body: InterestWithdrawalBodyDto
    ): Completable
}
