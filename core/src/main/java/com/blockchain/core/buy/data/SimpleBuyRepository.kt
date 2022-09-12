package com.blockchain.core.buy.data

import com.blockchain.core.buy.data.dataresources.BuyPairsStore
import com.blockchain.core.buy.data.dataresources.SimpleBuyEligibilityStore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairDto
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow

class SimpleBuyRepository(
    private val simpleBuyEligibilityStore: SimpleBuyEligibilityStore,
    private val buyPairsStore: BuyPairsStore
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
