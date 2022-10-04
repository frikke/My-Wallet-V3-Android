package com.blockchain.core.staking.data

import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.core.staking.data.datasources.StakingEligibilityStore
import com.blockchain.core.staking.data.datasources.StakingRatesStore
import com.blockchain.core.staking.domain.StakingService
import com.blockchain.core.staking.domain.model.StakingEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.store.mapData
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StakingRepository(
    private val stakingRatesStore: StakingRatesStore,
    private val stakingFeatureFlag: FeatureFlag,
    private val stakingEligibilityStore: StakingEligibilityStore
) : StakingService {

    // we use the rates endpoint to determine whether the user has access to staking cryptos
    override fun getAvailabilityForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(ticker)
        }

    override suspend fun getRateForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Double>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates[ticker]?.rate ?: 0.0
        }

    // TODO(dserrano) - STAKING - add StakingBalanceStore checks here
    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> {
        return flow {
            val ffEnabled = stakingFeatureFlag.coEnabled()
            if (ffEnabled) {
                emit(setOf(CryptoCurrency.ETHER))
            } else
                emit(emptySet())
        }
    }

    // TODO(dserrano) - STAKING - ask @Seba how this should be used in coinview
    override suspend fun getEligibilityForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingEligibility>> {
        return stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap[ticker]?.let { eligibility ->
                if (eligibility.isEligible) {
                    StakingEligibility.Eligible
                } else {
                    eligibility.reason.toIneligibilityReason()
                }
            } ?: StakingEligibility.Ineligible.default()
        }
    }

    private fun String.toIneligibilityReason(): StakingEligibility.Ineligible {
        return when {
            this.isEmpty() -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.DEFAULT_REASON_NONE -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.UNSUPPORTED_REGION -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.INVALID_ADDRESS -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.TIER_TOO_LOW -> StakingEligibility.Ineligible.KYC_TIER
            else -> StakingEligibility.Ineligible.OTHER
        }
    }
}
