package com.blockchain.core.custodial

import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.TradingBalance
import com.blockchain.api.services.TradingBalanceList
import com.blockchain.auth.AuthHeaderProvider
import com.blockchain.core.common.caching.TimedCacheRequest
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

internal class TradingBalanceRecord(
    val balances: Map<Currency, TradingAccountBalance> = emptyMap()
)

internal class TradingBalanceCallCache(
    private val balanceService: CustodialBalanceService,
    private val assetCatalogue: AssetCatalogue,
    private val authHeaderProvider: AuthHeaderProvider
) {
    private val refresh: () -> Single<TradingBalanceRecord> = {
        authHeaderProvider.getAuthHeader()
            .flatMap { balanceService.getTradingBalanceForAllAssets(it) }
            .map { buildRecordMap(it) }
            .onErrorReturn { TradingBalanceRecord() }
    }

    private fun buildRecordMap(balanceList: TradingBalanceList): TradingBalanceRecord =
        TradingBalanceRecord(
            balances = balanceList.mapNotNull { balance ->
                assetCatalogue.fromNetworkTicker(balance.assetTicker)?.let { currency ->
                    currency to balance.toTradingAccountBalance(currency)
                }
            }.toMap()
        )

    private val custodialBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = refresh
    )

    fun getTradingBalances() =
        custodialBalancesCache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}

private fun TradingBalance.toTradingAccountBalance(currency: Currency) =
    TradingAccountBalance(
        total = Money.fromMinor(currency, total),
        withdrawable = Money.fromMinor(currency, withdrawable),
        pending = Money.fromMinor(currency, pending),
        hasTransactions = true
    )
