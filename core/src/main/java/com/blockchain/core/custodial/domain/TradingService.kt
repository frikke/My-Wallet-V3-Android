package com.blockchain.core.custodial.domain

import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface TradingService {
    fun getBalances(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Observable<Map<Currency, TradingAccountBalance>>

    fun getBalanceFor(
        asset: Currency,
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Observable<TradingAccountBalance>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<Set<Currency>>

    fun markAsStale()
}
