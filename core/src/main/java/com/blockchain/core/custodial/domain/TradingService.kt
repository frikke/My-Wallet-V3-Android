package com.blockchain.core.custodial.domain

import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.refreshstrategy.RefreshStrategy
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface TradingService {
    fun getBalances(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<Map<Currency, TradingAccountBalance>>

    fun getBalanceFor(
        asset: Currency,
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Observable<TradingAccountBalance>

    fun getActiveAssets(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<Set<Currency>>
}
