package com.blockchain.core.interest.domain

import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.refreshstrategy.RefreshStrategy
import info.blockchain.balance.AssetInfo
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
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    /**
     * Returns [InterestAccountBalance] for [asset]
     */
    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<InterestAccountBalance>

    /**
     * Returns a list of all [AssetInfo] that have an interest balance
     */
    fun getActiveAssets(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<Set<AssetInfo>>

    /**
     * Returns all assets eligible for interest
     */
    fun getAllAvailableAssets(): Single<List<AssetInfo>>

    /**
     * returns a map composed of each [AssetInfo] with its [InterestEligibility]
     */
    fun getEligibilityForAssets(): Single<Map<AssetInfo, InterestEligibility>>
}
