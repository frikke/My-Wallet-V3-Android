package com.blockchain.api.staking

import com.blockchain.api.staking.data.StakingAddressDto
import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.api.staking.data.StakingLimitsMapDto
import com.blockchain.api.staking.data.StakingRatesDto
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMapLeft
import com.blockchain.outcome.map

class StakingApiService internal constructor(
    private val stakingApi: StakingApi
) {
    suspend fun getStakingRates(): Outcome<Exception, StakingRatesDto> =
        stakingApi.getStakingRates()

    suspend fun getStakingEligibility(): Outcome<Exception, Map<String, StakingEligibilityDto>> =
        stakingApi.getStakingEligibility().flatMapLeft {
            Outcome.Success(emptyMap())
        }

    // Response here can return a 204 (No-Content), in which case, the success of the Outcome would be null;
    // so we catch it and return an empty map if this is the case
    suspend fun getStakingBalances(): Outcome<Exception, Map<String, StakingBalanceDto>> =
        stakingApi.getAccountBalances().map { response ->
            response ?: emptyMap()
        }

    suspend fun getStakingLimits(fiatTicker: String): Outcome<Exception, StakingLimitsMapDto> =
        stakingApi.getTickerLimits(null, fiatTicker)

    suspend fun getAccountAddress(cryptoTicker: String): Outcome<Exception, StakingAddressDto> =
        stakingApi.getAddress(cryptoTicker)
}
