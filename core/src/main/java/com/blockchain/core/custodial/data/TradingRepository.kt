package com.blockchain.core.custodial.data

import com.blockchain.api.services.TradingBalance
import com.blockchain.core.custodial.data.store.TradingDataSource
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.data.FreshnessStrategy
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class TradingRepository(
    private val assetCatalogue: AssetCatalogue,
    private val tradingDataSource: TradingDataSource
) : TradingService {

    private fun getBalancesFlow(refreshStrategy: FreshnessStrategy): Flow<Map<Currency, TradingAccountBalance>> {
        return tradingDataSource.streamData(refreshStrategy)
            .mapData { details ->
                details.mapNotNull { balance ->
                    assetCatalogue.fromNetworkTicker(balance.assetTicker)?.let { currency ->
                        currency to balance.toTradingAccountBalance(currency)
                    }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(refreshStrategy: FreshnessStrategy): Observable<Map<Currency, TradingAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: Currency, refreshStrategy: FreshnessStrategy): Observable<TradingAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<Currency>> {
        return getBalancesFlow(refreshStrategy)
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
