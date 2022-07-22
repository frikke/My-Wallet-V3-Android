package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.refreshstrategy.RefreshStrategy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface Erc20StoreService {
    fun getBalances(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        asset: AssetInfo,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<Set<AssetInfo>>
}
