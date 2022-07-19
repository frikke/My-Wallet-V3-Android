package com.blockchain.core.custodial.data

import com.blockchain.api.services.TradingBalance
import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.custodial.data.store.TradingDataSource
import com.blockchain.core.custodial.domain.TradingStoreService
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class TradingStoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val tradingDataSource: TradingDataSource
) : TradingStoreService {

    private fun getBalances(refresh: Boolean): Observable<Map<Currency, TradingAccountBalance>> {
        return tradingDataSource.stream(refresh)
            .mapData { details ->
                details.mapNotNull { balance ->
                    assetCatalogue.fromNetworkTicker(balance.assetTicker)?.let { currency ->
                        currency to balance.toTradingAccountBalance(currency)
                    }
                }.toMap()
            }
            .asObservable { it }
            .onErrorReturn { emptyMap() }
    }

    override fun getBalances(): Observable<Map<Currency, TradingAccountBalance>> =
        getBalances(refresh = true)

    override fun getBalanceFor(asset: Currency): Observable<TradingAccountBalance> {
        return getBalances(refresh = true)
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(forceRefresh: Boolean): Single<Set<Currency>> {
        return getBalances(forceRefresh)
            .map { it.keys }.firstElement().toSingle()
    }
}

private fun TradingBalance.toTradingAccountBalance(currency: Currency) =
    TradingAccountBalance(
        total = Money.fromMinor(currency, total),
        withdrawable = Money.fromMinor(currency, withdrawable),
        pending = Money.fromMinor(currency, pending),
        hasTransactions = true
    )

private fun zeroBalance(currency: Currency): TradingAccountBalance =
    TradingAccountBalance(
        total = Money.zero(currency),
        withdrawable = Money.zero(currency),
        pending = Money.zero(currency)
    )
