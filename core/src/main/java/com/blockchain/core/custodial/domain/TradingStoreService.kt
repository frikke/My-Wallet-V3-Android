package com.blockchain.core.custodial.domain

import com.blockchain.core.custodial.TradingAccountBalance
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface TradingStoreService {
    fun getBalances(): Observable<Map<Currency, TradingAccountBalance>>
    fun getBalanceFor(asset: Currency): Observable<TradingAccountBalance>
    fun getActiveAssets(forceRefresh: Boolean = false): Single<Set<Currency>>
}
