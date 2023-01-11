package com.blockchain.earn.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.models.staking.StakingActivity
import com.blockchain.earn.domain.models.staking.StakingLimits
import com.blockchain.earn.domain.models.staking.StakingRates
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface StakingService {

    val defFreshness
        get() = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(10, TimeUnit.MINUTES)
        )

    fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Boolean>>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<Set<AssetInfo>>

    fun getBalanceForAllAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, StakingAccountBalance>>>

    fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<StakingAccountBalance>>

    fun getRatesForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<StakingRates>>

    fun getRatesForAllAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, Double>>>

    fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, StakingEligibility>>>

    fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<StakingEligibility>>

    fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<StakingEligibility>>

    fun getActivity(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<StakingActivity>>>

    fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, StakingLimits>>>

    suspend fun getAccountAddress(currency: Currency): DataResource<String>

    fun getAccountAddressRx(currency: Currency): Single<String>

    fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<StakingLimits>>
}
