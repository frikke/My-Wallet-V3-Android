package com.blockchain.api.staking

import com.blockchain.api.staking.data.StakingRatesDto
import com.blockchain.outcome.Outcome

class StakingApiService internal constructor(
    private val stakingApi: StakingApi
) {
    suspend fun getStakingRates(): Outcome<Exception, StakingRatesDto> =
        stakingApi.getStakingRates()

    // TODO(dserrano) - STAKING
//    suspend fun getAccountBalances(): Outcome<Exception, Map<String, StakingAccountBalanceDto>> =
//        stakingApi.getAccountBalances()
}
