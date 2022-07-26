package com.blockchain.core.custodial.data

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.refreshstrategy.RefreshStrategy
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.store.toStoreRequest
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

    private fun getBalancesFlow(refreshStrategy: RefreshStrategy): Flow<Map<Currency, TradingAccountBalance>> {
        return tradingStore.stream(refreshStrategy.toStoreRequest())
            .mapData { tradingBalancesWithAssets: Map<String, TradingBalanceResponseDto> ->
                tradingBalancesWithAssets.mapNotNull { (assetTicker, tradingBalanceResponse) ->
                    assetCatalogue.fromNetworkTicker(assetTicker)?.let { currency ->
                        currency to tradingBalanceResponse.toTradingAccountBalance(currency)
                    }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(refreshStrategy: RefreshStrategy): Observable<Map<Currency, TradingAccountBalance>> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: Currency, refreshStrategy: RefreshStrategy): Observable<TradingAccountBalance> {
        return getBalancesFlow(refreshStrategy)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(refreshStrategy: RefreshStrategy): Flow<Set<Currency>> {
        return getBalancesFlow(refreshStrategy)
            .map { it.keys }
    }
}

private fun TradingBalanceResponseDto.toTradingAccountBalance(currency: Currency) =
    TradingAccountBalance(
        total = Money.fromMinor(currency, total.toBigInteger()),
        withdrawable = Money.fromMinor(currency, withdrawable.toBigInteger()),
        pending = Money.fromMinor(currency, pending.toBigInteger()),
        hasTransactions = true
    )

private fun zeroBalance(currency: Currency): TradingAccountBalance =
    TradingAccountBalance(
        total = Money.zero(currency),
        withdrawable = Money.zero(currency),
        pending = Money.zero(currency)
    )
