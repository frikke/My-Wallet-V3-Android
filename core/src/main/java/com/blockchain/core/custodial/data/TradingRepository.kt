package com.blockchain.core.custodial.data

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.getDataOrThrow
import com.blockchain.data.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class TradingRepository(
    private val assetCatalogue: AssetCatalogue,
    private val tradingStore: TradingStore
) : TradingService {

    private fun getBalancesFlow(refreshStrategy: FreshnessStrategy): Flow<Map<Currency, TradingAccountBalance>> {
        return tradingStore.stream(refreshStrategy)
            .mapData { tradingBalancesWithAssets: Map<String, TradingBalanceResponseDto> ->
                tradingBalancesWithAssets.mapNotNull { (assetTicker, tradingBalanceResponse) ->
                    assetCatalogue.fromNetworkTicker(assetTicker)?.let { currency ->
                        currency to tradingBalanceResponse.toTradingAccountBalance(currency)
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

    override fun markAsStale() {
        tradingStore.markAsStale()
    }
}

private fun TradingBalanceResponseDto.toTradingAccountBalance(currency: Currency) =
    TradingAccountBalance(
        total = Money.fromMinor(currency, total.toBigInteger()),
        withdrawable = Money.fromMinor(currency, withdrawable.toBigInteger()),
        pending = Money.fromMinor(currency, pending.toBigInteger()),
        dashboardDisplay = Money.fromMinor(currency, mainBalanceToDisplay.toBigInteger()),
        hasTransactions = true
    )

private fun zeroBalance(currency: Currency): TradingAccountBalance =
    TradingAccountBalance(
        total = Money.zero(currency),
        withdrawable = Money.zero(currency),
        pending = Money.zero(currency),
        dashboardDisplay = Money.zero(currency)
    )
