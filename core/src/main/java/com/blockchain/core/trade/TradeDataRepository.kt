package com.blockchain.core.trade

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.trade.model.RecurringBuy
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.android.data.toDomain

class TradeDataRepository(
    private val tradeService: TradeService,
    private val getRecurringBuysStore: GetRecurringBuysStore,
    private val assetCatalogue: AssetCatalogue
) : TradeDataService {

    override fun isFirstTimeBuyer(): Single<Boolean> =
        tradeService.isFirstTimeBuyer()
            .map { accumulatedInPeriod ->
                accumulatedInPeriod.tradesAccumulated
                    .first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toDouble() == 0.0
            }

    override fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>> =
        tradeService.getNextPaymentDate().map { it.toDomain() }

    override fun getRecurringBuysForAsset(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<RecurringBuy>>> {
        return getRecurringBuysStore
            .stream(
                freshnessStrategy.withKey(GetRecurringBuysStore.Key(asset.networkTicker))
            )
            .mapData { recurringBuyResponses ->
                recurringBuyResponses.mapNotNull { it.toDomain(assetCatalogue) }
            }
    }

    override fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy> =
        tradeService.getRecurringBuyForId(recurringBuyId)
            .map { recurringBuyResponses ->
                recurringBuyResponses.mapNotNull { it.toDomain(assetCatalogue) }.first()
            }

    override fun cancelRecurringBuy(recurringBuy: RecurringBuy): Completable =
        tradeService.cancelRecurringBuy(recurringBuy.id)
            .doOnComplete {
                getRecurringBuysStore.markAsStale(GetRecurringBuysStore.Key(recurringBuy.asset.networkTicker))
            }

    override fun getQuotePrice(
        currencyPair: String,
        amount: String,
        paymentMethod: String,
        orderProfileName: String
    ): Observable<QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair,
            amount = amount,
            paymentMethod = paymentMethod,
            orderProfileName = orderProfileName
        ).map { response ->
            response.toDomain(assetCatalogue)
        }
}
