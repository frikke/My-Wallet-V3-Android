package com.blockchain.earn.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.StakingRewardsRates
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingActivity
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface StakingService {
    fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfStale
        )
    ): Flow<DataResource<Boolean>>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<Set<AssetInfo>>

    fun getBalanceForAllAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<Map<AssetInfo, StakingAccountBalance>>>

    fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<StakingAccountBalance>>

    fun getRatesForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<StakingRewardsRates>>

    fun getRatesForAllAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<Map<AssetInfo, Double>>>

    fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<Map<AssetInfo, EarnRewardsEligibility>>>

    fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<EarnRewardsEligibility>>

    fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<EarnRewardsEligibility>>

    fun getActivity(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<EarnRewardsActivity>>>

    fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<Map<AssetInfo, StakingLimits>>>

    suspend fun getAccountAddress(currency: Currency): Outcome<Exception, String>

    fun getAccountAddressRx(currency: Currency): Single<String>

    fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<StakingLimits>>

    suspend fun getPendingActivity(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<StakingActivity>>>
}
