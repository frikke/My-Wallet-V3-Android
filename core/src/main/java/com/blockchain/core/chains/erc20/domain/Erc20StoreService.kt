package com.blockchain.core.chains.erc20.domain

import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.StoreRequest
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface Erc20StoreService {
    fun getBalances(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Map<AssetInfo, Erc20Balance>>

    fun getBalanceFor(
        asset: AssetInfo,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Erc20Balance>

    fun getActiveAssets(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>
}
