package com.blockchain.earn.domain.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestActivity
import com.blockchain.earn.domain.models.interest.InterestEligibility
import com.blockchain.earn.domain.models.interest.InterestLimits
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
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
    fun getBalances(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestAccountBalance]
     */
    fun getBalancesFlow(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Map<AssetInfo, InterestAccountBalance>>>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Observable<InterestAccountBalance>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceForFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<InterestAccountBalance>>

    /**
     * Returns a list of all [AssetInfo] that have an interest balance
     */
    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<Set<AssetInfo>>

    /**
     * Returns all assets that can earn rewards
     * This list doesn't mean that all assets are eligible, some can be [InterestEligibility.Ineligible]
     *
     * @see [getEligibilityForAssetsLegacy]
     */
    fun getAvailableAssetsForInterest(): Single<List<AssetInfo>>

    /**
     * Returns all assets that can earn rewards
     * This list doesn't mean that all assets are eligible, some can be [InterestEligibility.Ineligible]
     *
     * @see [getEligibilityForAssetsLegacy]
     */
    fun getAvailableAssetsForInterestFlow(): Flow<DataResource<List<AssetInfo>>>

    /**
     * Returns if an [asset] can earn rewards
     * True doesn't mean the asset is eligible, it can be [InterestEligibility.Ineligible]
     */
    fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean>

    /**
     * Returns if an [asset] can earn rewards
     * True doesn't mean the asset is eligible, it can be [InterestEligibility.Ineligible]
     */
    fun isAssetAvailableForInterestFlow(
        asset: AssetInfo
    ): Flow<DataResource<Boolean>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestEligibility]
     */
    @Deprecated("use flow getEligibilityForAssets")
    fun getEligibilityForAssetsLegacy(): Single<Map<AssetInfo, InterestEligibility>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestEligibility]
     */
    fun getEligibilityForAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Map<AssetInfo, InterestEligibility>>>

    /**
     * Returns [InterestEligibility] for [asset]
     */
    fun getEligibilityForAsset(asset: AssetInfo): Single<InterestEligibility>

    /**
     * Returns [InterestEligibility] for [asset]
     */
    fun getEligibilityForAssetFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<InterestEligibility>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestLimits]
     */
    fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestLimits]
     */
    fun getLimitsForAssetsFlow(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
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
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
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
    fun getActivity(asset: AssetInfo): Single<List<InterestActivity>>

    /**
     * Returns a list of transactions for [asset]
     */
    fun getActivityFlow(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<InterestActivity>>>

    /**
     * Executes interest withdrawal of [asset]:[amount] to [address]
     * @see [InterestWithdrawOnChainTxEngine]
     * todo: coroutines
     */
    fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable
}
