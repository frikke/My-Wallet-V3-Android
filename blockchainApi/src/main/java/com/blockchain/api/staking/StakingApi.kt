package com.blockchain.api.staking

import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.api.staking.data.StakingRatesDto
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

internal interface StakingApi {

    @GET("earn/eligible")
    suspend fun getStakingEligibility(
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, Map<String, StakingEligibilityDto>>

    @GET("earn/rates-user")
    suspend fun getStakingRates(
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, StakingRatesDto>

    @GET("accounts/staking")
    suspend fun getAccountBalances(): Outcome<Exception, Map<String, StakingBalanceDto>?>

    // TODO(dserrano) - STAKING
    //    @GET("earn/limits")
    //    suspend fun getTickersLimits(
    //        @Query("currency") ticker: String
    //    ): Outcome<Exception, StakingAssetLimitsDto>
}
