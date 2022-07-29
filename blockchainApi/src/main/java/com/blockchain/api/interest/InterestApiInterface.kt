package com.blockchain.api.interest

import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.api.interest.data.InterestAddressDto
import com.blockchain.api.interest.data.InterestAvailableTickersDto
import com.blockchain.api.interest.data.InterestEligibilityDto
import com.blockchain.api.interest.data.InterestRateDto
import com.blockchain.api.interest.data.InterestTickerLimitsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private const val BALANCES = "accounts/savings"
private const val AVAILABLE_TICKERS = "savings/instruments"
private const val ELIGIBILITY = "eligible/product/savings"
private const val LIMITS = "savings/limits"
private const val INTEREST_RATES = "savings/rates"
private const val ADDRESS = "payments/accounts/savings"

internal interface InterestApiInterface {
    @GET(BALANCES)
    fun getAccountBalances(
        @Header("authorization") authorization: String
    ): Single<Response<Map<String, InterestAccountBalanceDto>>>

    @GET(AVAILABLE_TICKERS)
    fun getAvailableTickersForInterest(
        @Header("authorization") authorization: String
    ): Single<InterestAvailableTickersDto>

    @GET(ELIGIBILITY)
    fun getTickersEligibility(
        @Header("authorization") authorization: String
    ): Single<Map<String, InterestEligibilityDto>>

    @GET(LIMITS)
    fun getTickersLimits(
        @Header("authorization") authorization: String,
        @Query("currency") fiatCurrencyTicker: String
    ): Single<InterestTickerLimitsDto>

    @GET(INTEREST_RATES)
    fun getInterestRates(
        @Header("authorization") authorization: String,
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<Response<InterestRateDto>>

    @GET(ADDRESS)
    fun getAddress(
        @Header("authorization") authorization: String,
        @Query("ccy") cryptoCurrencyTicker: String
    ): Single<InterestAddressDto>
}
