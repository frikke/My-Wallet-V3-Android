package com.blockchain.core.custodial

import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class TradingAccountBalance(
    val total: Money,
    val withdrawable: Money,
    val pending: Money,
    val hasTransactions: Boolean = false
)

interface TradingBalanceDataManager {
    fun getBalanceForCurrency(currency: Currency): Observable<TradingAccountBalance>
    fun getActiveAssets(): Single<Set<Currency>>
}

internal class TradingBalanceDataManagerImpl(
    private val balanceCallCache: TradingBalanceCallCache
) : TradingBalanceDataManager {
    override fun getBalanceForCurrency(currency: Currency): Observable<TradingAccountBalance> =
        balanceCallCache.getTradingBalances()
            .map { it.balances.getOrDefault(currency, zeroBalance(currency)) }
            .toObservable()

    override fun getActiveAssets(): Single<Set<Currency>> =
        balanceCallCache.getTradingBalances()
            .map { it.balances.keys }
}

private fun zeroBalance(currency: Currency): TradingAccountBalance =
    TradingAccountBalance(
        total = Money.zero(currency),
        withdrawable = Money.zero(currency),
        pending = Money.zero(currency)
    )
