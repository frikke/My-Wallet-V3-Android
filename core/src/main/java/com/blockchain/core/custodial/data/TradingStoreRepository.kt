package com.blockchain.core.custodial.data

import com.blockchain.api.services.TradingBalance
import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.custodial.data.store.TradingDataSource
import com.blockchain.core.custodial.domain.TradingStoreService
import com.blockchain.store.StoreRequest
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class TradingStoreRepository(
    private val assetCatalogue: AssetCatalogue,
    private val tradingDataSource: TradingDataSource
) : TradingStoreService {

    private fun getBalancesFlow(storeRequest: StoreRequest): Flow<Map<Currency, TradingAccountBalance>> {
        return tradingDataSource.stream(storeRequest)
            .mapData { details ->
                details.mapNotNull { balance ->
                    assetCatalogue.fromNetworkTicker(balance.assetTicker)?.let { currency ->
                        currency to balance.toTradingAccountBalance(currency)
                    }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(request: StoreRequest): Observable<Map<Currency, TradingAccountBalance>> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: Currency, request: StoreRequest): Observable<TradingAccountBalance> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(request: StoreRequest): Flow<Set<Currency>> {
        return getBalancesFlow(request)
            .map { it.keys }
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
