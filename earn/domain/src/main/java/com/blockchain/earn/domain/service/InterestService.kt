package com.blockchain.earn.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnRewardsActivity
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestLimits
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

/**
 * Interest Operations
 *
 * **_only for custodial (trading) assets_**
 */
interface InterestService {
    /**
     * Returns a map composed of each [AssetInfo] with its [InterestAccountBalance]
     */

    val defFreshness
        get() = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )

    fun getBalances(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestAccountBalance]
     */
    fun getBalancesFlow(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, InterestAccountBalance>>>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Observable<InterestAccountBalance>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceForFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<InterestAccountBalance>>

    /**
     * Returns a list of all [AssetInfo] that have an interest balance
     */
    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<Set<AssetInfo>>

    /**
     * Returns all assets that can earn rewards
     * This list doesn't mean that all assets are eligible, some can be [EarnRewardsEligibility.Ineligible]
     *
     * @see [getEligibilityForAssetsLegacy]
     */
    fun getAvailableAssetsForInterest(): Single<List<AssetInfo>>

    /**
     * Returns all assets that can earn rewards
     * This list doesn't mean that all assets are eligible, some can be [EarnRewardsEligibility.Ineligible]
     *
     * @see [getEligibilityForAssetsLegacy]
     */
    fun getAvailableAssetsForInterestFlow(): Flow<DataResource<List<AssetInfo>>>

    /**
     * Returns if an [asset] can earn rewards
     * True doesn't mean the asset is eligible, it can be [EarnRewardsEligibility.Ineligible]
     */
    fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean>

    /**
     * Returns if an [asset] can earn rewards
     * True doesn't mean the asset is eligible, it can be [EarnRewardsEligibility.Ineligible]
     */
    fun isAssetAvailableForInterestFlow(
        asset: AssetInfo
    ): Flow<DataResource<Boolean>>

    /**
     * Returns a map composed of each [AssetInfo] with its [EarnRewardsEligibility]
     */
    @Deprecated("use flow getEligibilityForAssets")
    fun getEligibilityForAssetsLegacy(): Single<Map<AssetInfo, EarnRewardsEligibility>>

    /**
     * Returns a map composed of each [AssetInfo] with its [EarnRewardsEligibility]
     */
    fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, EarnRewardsEligibility>>>

    /**
     * Returns [EarnRewardsEligibility] for [asset]
     */
    fun getEligibilityForAsset(asset: AssetInfo): Single<EarnRewardsEligibility>

    /**
     * Returns [EarnRewardsEligibility] for [asset]
     */
    fun getEligibilityForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<EarnRewardsEligibility>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestLimits]
     */
    fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestLimits]
     */
    fun getLimitsForAssetsFlow(
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Map<AssetInfo, InterestLimits>>>

    /**
     * Returns [InterestLimits] for [asset]
     */
    fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits>

    /**
     * Returns [InterestLimits] for [asset]
     */
    fun getLimitsForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<InterestLimits>>

    /**
     * Returns the interest rate for [asset]
     */
    fun getInterestRate(asset: AssetInfo): Single<Double>

    /**
     * Returns the interest rate for [asset]
     */
    fun getInterestRateFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<Double>>

    fun getAllInterestRates(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<Map<AssetInfo, Double>>>

    /**
     * Returns the address for [asset]
     * todo: no cache for this - change to coroutines
     */
    fun getAddress(asset: AssetInfo): Single<String>

    /**
     * Returns a list of transactions for [asset]
     */
    fun getActivity(asset: AssetInfo): Single<List<EarnRewardsActivity>>

    /**
     * Returns a list of transactions for [asset]
     */
    fun getActivityFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<EarnRewardsActivity>>>

    /**
     * Executes interest withdrawal of [asset]:[amount] to [address]
     * @see [InterestWithdrawOnChainTxEngine]
     * todo: coroutines
     */
    fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable

    fun markBalancesAsStale()
}
