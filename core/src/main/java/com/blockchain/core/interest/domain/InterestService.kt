package com.blockchain.core.interest.domain

import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.refreshstrategy.RefreshStrategy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface InterestService {
    fun getBalances(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<InterestAccountBalance>

    fun getActiveAssets(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<Set<AssetInfo>>

    fun getEnabledStatusForAllAssets(): Single<List<AssetInfo>>
}
