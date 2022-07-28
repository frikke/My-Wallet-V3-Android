package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface Erc20StoreService {
    fun getBalances(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>
}
