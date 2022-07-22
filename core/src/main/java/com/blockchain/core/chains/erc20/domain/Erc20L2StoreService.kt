package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.refreshstrategy.RefreshStrategy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface Erc20L2StoreService {
    fun getBalances(
        networkTicker: String,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        networkTicker: String,
        asset: AssetInfo,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        networkTicker: String,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<Set<AssetInfo>>
}
