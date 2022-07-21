package com.blockchain.core.interest.domain

import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.store.StoreRequest
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface InterestService {
    fun getBalances(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Map<AssetInfo, InterestAccountBalance>>

    fun getBalanceFor(
        asset: AssetInfo,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<InterestAccountBalance>

    fun getActiveAssets(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>
}
