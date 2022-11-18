package com.blockchain.core.staking.domain

import com.blockchain.core.staking.domain.model.StakingAccountBalance
import com.blockchain.core.staking.domain.model.StakingEligibility
import com.blockchain.core.staking.domain.model.StakingLimits
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

interface StakingService {

    fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>

    fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<StakingAccountBalance>>

    fun getRateForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Double>>

    fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<StakingEligibility>>

    fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<StakingEligibility>>

    fun getActivity(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<List<StakingActivity>>>

    fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Map<AssetInfo, StakingLimits>>>

    fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<StakingLimits>>
}
