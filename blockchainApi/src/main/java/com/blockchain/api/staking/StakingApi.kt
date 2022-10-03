package com.blockchain.api.staking

import com.blockchain.api.staking.data.StakingRatesDto
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

internal interface StakingApi {

    // TODO(dserrano) - STAKING

//    @GET("accounts/staking")
    //    suspend fun getAccountBalances(): Outcome<Exception, Map<String, StakingAccountBalanceDto>>
    //
    //    @GET("earn/eligible")
    //    suspend fun getTickersEligibility(): Outcome<Exception, Map<String, StakingEligibilityDto>>
    //
    //    @GET("earn/limits")
    //    suspend fun getTickersLimits(
    //        @Query("currency") ticker: String
    //    ): Outcome<Exception, StakingAssetLimitsDto>
    //
    //    @GET("earn/rates-user")
    //    suspend fun getStakingRatesForCurrency(
    //        @Query("ccy") cryptoCurrencyTicker: String
    //    ): Outcome<Exception, StakingRatesDto>

    @GET("earn/rates-user")
    suspend fun getStakingRates(
        @Query("product") product: String = "STAKING"
    ): Outcome<Exception, StakingRatesDto>
}
