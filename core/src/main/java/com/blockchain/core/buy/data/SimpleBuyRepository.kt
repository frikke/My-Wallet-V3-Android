package com.blockchain.core.buy.data

import com.blockchain.core.buy.data.dataresources.BuyPairsStore
import com.blockchain.core.buy.data.dataresources.SimpleBuyEligibilityStore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.datamanagers.BuySellLimits
import com.blockchain.nabu.datamanagers.BuySellPair
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairDto
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import kotlinx.coroutines.flow.Flow

class SimpleBuyRepository(
    private val simpleBuyEligibilityStore: SimpleBuyEligibilityStore,
    private val buyPairsStore: BuyPairsStore,
    private val assetCatalogue: AssetCatalogue
) : SimpleBuyService {

    override fun getEligibility(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<SimpleBuyEligibility>> {
        return simpleBuyEligibilityStore.stream(freshnessStrategy)
            .mapData { it.toDomain() }
    }

    override fun isEligible(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Boolean>> {
        return getEligibility(freshnessStrategy).mapData { it.simpleBuyTradingEligible }
    }

    override fun getPairs(freshnessStrategy: FreshnessStrategy): Flow<DataResource<List<SimpleBuyPair>>> {
        return buyPairsStore.stream(freshnessStrategy).mapData {
            it.pairs.map { it.toDomain() }
        }
    }

    override fun getSupportedBuySellCryptoCurrencies(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<CurrencyPair>>> {
        return buyPairsStore.stream(freshnessStrategy).mapData { response ->
            response.pairs.mapNotNull { pair ->
                pair.toBuySellPair()?.let {
                    CurrencyPair(source = it.cryptoCurrency, destination = it.fiatCurrency)
                }
            }
        }
    }

    private fun SimpleBuyPairDto.toBuySellPair(): BuySellPair? {
        val parts = pair.split("-")
        val crypto = parts.getOrNull(0)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }
        val fiat = parts.getOrNull(1)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }

        return if (crypto == null || fiat == null) {
            null
        } else {
            BuySellPair(
                cryptoCurrency = crypto,
                fiatCurrency = fiat,
                buyLimits = BuySellLimits(buyMin.toBigInteger(), buyMax.toBigInteger()),
                sellLimits = BuySellLimits(sellMin.toBigInteger(), sellMax.toBigInteger())
            )
        }
    }

    private fun SimpleBuyEligibilityDto.toDomain() = run {
        SimpleBuyEligibility(
            eligible = eligible,
            simpleBuyTradingEligible = simpleBuyTradingEligible,
            pendingDepositSimpleBuyTrades = pendingDepositSimpleBuyTrades,
            maxPendingDepositSimpleBuyTrades = maxPendingDepositSimpleBuyTrades
        )
    }

    private fun SimpleBuyPairDto.toDomain() = run {
        SimpleBuyPair(
            pair = pair.split("-").run { Pair(first(), last()) },
            buyMin = buyMin,
            buyMax = buyMax,
            sellMin = sellMin,
            sellMax = sellMax
        )
    }
}
