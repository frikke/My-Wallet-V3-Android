package com.blockchain.core.interest.domain

import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.interest.domain.model.InterestLimits
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.datamanagers.InterestActivityItem
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
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<InterestAccountBalance>

    /**
     * Returns a list of all [AssetInfo] that have an interest balance
     */
    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>

    /**
     * Returns all assets that can earn rewards
     * This list doesn't mean that all assets are eligible, some can be [InterestEligibility.Ineligible]
     *
     * @see [getEligibilityForAssets]
     */
    fun getAvailableAssetsForInterest(): Single<List<AssetInfo>>

    /**
     * Returns if an [asset] can earn rewards
     * True doesn't mean the asset is eligible, it can be [InterestEligibility.Ineligible]
     */
    fun isAssetAvailableForInterest(asset: AssetInfo): Single<Boolean>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestEligibility]
     */
    fun getEligibilityForAssets(): Single<Map<AssetInfo, InterestEligibility>>

    /**
     * Returns [InterestEligibility] for [asset]
     */
    fun getEligibilityForAsset(asset: AssetInfo): Single<InterestEligibility>

    /**
     * Returns a map composed of each [AssetInfo] with its [InterestLimits]
     */
    fun getLimitsForAssets(): Single<Map<AssetInfo, InterestLimits>>

    /**
     * Returns [InterestLimits] for [asset]
     */
    fun getLimitsForAsset(asset: AssetInfo): Single<InterestLimits>

    /**
     * Returns the interest rate for [asset]
     */
    fun getInterestRate(asset: AssetInfo): Single<Double>

    /**
     * Returns the address for [asset]
     */
    fun getAddress(asset: AssetInfo): Single<String>

    /**
     * Returns a list of transactions for [asset]
     */
    fun getActivity(asset: AssetInfo): Single<List<InterestActivityItem>>

    /**
     * Executes interest withdrawal of [asset]:[amount] to [address]
     * @see [InterestWithdrawOnChainTxEngine]
     */
    fun withdraw(asset: AssetInfo, amount: Money, address: String): Completable
}
