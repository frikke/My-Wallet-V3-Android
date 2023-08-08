package com.blockchain.api.earn.staking

import com.blockchain.api.earn.EarnRewardsEligibilityDto
import com.blockchain.api.earn.staking.data.StakingActivityDto
import com.blockchain.api.earn.staking.data.StakingAddressDto
import com.blockchain.api.earn.staking.data.StakingBalanceDto
import com.blockchain.api.earn.staking.data.StakingLimitsMapDto
import com.blockchain.api.earn.staking.data.StakingRatesDto
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

internal interface StakingApi {

    @GET("earn/eligible")
    suspend fun getStakingEligibility(
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, Map<String, EarnRewardsEligibilityDto>>

    @GET("earn/rates-user")
    suspend fun getStakingRates(
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, StakingRatesDto>

    @GET("accounts/staking")
    suspend fun getAccountBalances(): Outcome<Exception, Map<String, StakingBalanceDto>?>

    @GET("earn/limits")
    suspend fun getTickerLimits(
        @Query("ccy") cryptoTicker: String?,
        @Query("currency") fiatTicker: String,
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, StakingLimitsMapDto>

    @GET("payments/accounts/staking")
    suspend fun getAddress(
        @Query("ccy") cryptoCurrencyTicker: String
    ): Outcome<Exception, StakingAddressDto>

    @GET("earn/bonding-txs")
    suspend fun getWithdrawalRequests(
        @Query("product") product: String = "STAKING",
        @Query("ccy") cryptoCurrencyTicker: String
    ): Outcome<Exception, StakingActivityDto>
}
