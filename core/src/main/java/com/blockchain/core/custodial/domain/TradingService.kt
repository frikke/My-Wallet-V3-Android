package com.blockchain.core.custodial.domain

import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface TradingService {
    fun getBalances(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<Map<Currency, TradingAccountBalance>>

    fun getBalanceFor(
        asset: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Observable<TradingAccountBalance>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<Set<Currency>>
}
