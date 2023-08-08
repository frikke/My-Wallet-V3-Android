package com.blockchain.api.earn.active

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.active.data.ActiveRewardsAddressDto
import com.blockchain.api.earn.active.data.ActiveRewardsBalanceDto
import com.blockchain.api.earn.active.data.ActiveRewardsLimitsMapDto
import com.blockchain.api.earn.active.data.ActiveRewardsRatesDto
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

internal interface ActiveRewardsApi {

    @GET("earn/eligible")
    suspend fun getActiveRewardsEligibility(
        @Query("product") product: String = "EARN_CC1W"
    ): Outcome<Exception, Map<String, EarnRewardsEligibilityDto>>

    @GET("earn/rates-user")
    suspend fun getActiveRewardsRates(
        @Query("product") product: String = "EARN_CC1W"
    ): Outcome<Exception, ActiveRewardsRatesDto>

    @GET("accounts/earn_cc1w")
    suspend fun getAccountBalances(): Outcome<Exception, Map<String, ActiveRewardsBalanceDto>?>

    @GET("earn/limits")
    suspend fun getTickerLimits(
        @Query("ccy") cryptoTicker: String?,
        @Query("currency") fiatTicker: String,
        @Query("product") product: String = "EARN_CC1W"
    ): Outcome<Exception, ActiveRewardsLimitsMapDto>

    @GET("earn/withdrawal-requests")
    suspend fun getWithdrawalRequests(
        @Query("product") product: String = "EARN_CC1W"
    ): Outcome<Exception, List<EarnWithdrawalDto>>

    @GET("payments/accounts/earn_cc1w")
    suspend fun getAddress(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Outcome<Exception, ActiveRewardsAddressDto>
}
