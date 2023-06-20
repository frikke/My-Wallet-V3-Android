package com.blockchain.earn.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.ActiveRewardsRates
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.active.ActiveRewardsLimits
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow

interface ActiveRewardsService {

    fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<Boolean>>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<Set<AssetInfo>>

    fun getBalanceForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, ActiveRewardsAccountBalance>>>

    fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<ActiveRewardsAccountBalance>>

    fun getRatesForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<ActiveRewardsRates>>

    fun getRatesForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, Double>>>

    fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, EarnRewardsEligibility>>>

    fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>>

    fun getEarnRewardsEligibility(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<EarnRewardsEligibility>>

    fun getActivity(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<EarnRewardsActivity>>>

    fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, ActiveRewardsLimits>>>

    suspend fun getAccountAddress(currency: Currency): Outcome<Exception, String>

    fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<ActiveRewardsLimits>>

    suspend fun hasOngoingWithdrawals(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Boolean>>

    fun markBalancesAsStale()
}
