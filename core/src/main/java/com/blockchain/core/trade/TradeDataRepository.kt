package com.blockchain.core.trade

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.trade.model.RecurringBuy
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
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

    override fun getBuyQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        paymentMethod: PaymentMethodType,
    ): Single<QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = paymentMethod.name,
            orderProfileName = "SIMPLEBUY"
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    override fun getSellQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Single<QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            orderProfileName = direction.getQuoteOrderProfileName(),
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    override fun getSwapQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Single<QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            orderProfileName = direction.getQuoteOrderProfileName(),
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    private fun TransferDirection.getQuotePaymentMethod(): String =
        if (this == TransferDirection.INTERNAL) "FUNDS" else "DEPOSIT"

    private fun TransferDirection.getQuoteOrderProfileName(): String = when (this) {
        TransferDirection.ON_CHAIN -> "SWAP_ON_CHAIN"
        TransferDirection.FROM_USERKEY -> "SWAP_FROM_USERKEY"
        TransferDirection.TO_USERKEY -> throw UnsupportedOperationException()
        TransferDirection.INTERNAL -> "SWAP_INTERNAL"
    }
}
