package com.blockchain.core.custodial.domain

import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.store.StoreRequest
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface TradingStoreService {
    fun getBalances(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<Map<Currency, TradingAccountBalance>>

    fun getBalanceFor(
        asset: Currency,
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Observable<TradingAccountBalance>

    fun getActiveAssets(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<Set<Currency>>
}
