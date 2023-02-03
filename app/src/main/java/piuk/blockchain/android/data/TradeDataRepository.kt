package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.QuoteResponse
import com.blockchain.core.recurringbuy.domain.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.simplebuy.BuyQuote.Companion.toFiat

// TODO(aromano): move to core
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
            val currencyPair = CurrencyPair.fromRawPair(
                rawValue = response.currencyPair,
                assetCatalogue = assetCatalogue
            )
            require(currencyPair != null) { "CurrencyPair in GetQuote is null" }
            QuotePrice(
                currencyPair = currencyPair,
                amount = Money.fromMinor(
                    currency = currencyPair.source,
                    value = response.amount.toBigInteger()
                ),
                price = Money.fromMinor(
                    currency = currencyPair.destination,
                    value = response.price.toBigInteger()
                ),
                resultAmount = Money.fromMinor(
                    currency = currencyPair.destination,
                    value = response.resultAmount.toBigInteger()
                ),
                dynamicFee = Money.fromMinor(
                    currency = currencyPair.source,
                    value = response.dynamicFee.toBigInteger()
                ),
                networkFee = response.networkFee?.let {
                    Money.fromMinor(
                        currency = currencyPair.source,
                        value = it.toBigInteger()
                    )
                },
                paymentMethod = response.paymentMethod.toPaymentMethodType(),
                orderProfileName = response.orderProfileName
            )
        }
}

data class QuotePrice(
    val currencyPair: CurrencyPair,
    val amount: Money,
    val price: Money,
    val resultAmount: Money,
    val dynamicFee: Money,
    val networkFee: Money?,
    val paymentMethod: PaymentMethodType,
    val orderProfileName: String
) {
    val fiatPrice: FiatValue
        get() = price.toFiat(currencyPair.source)
}

private fun String.toPaymentMethodType(): PaymentMethodType = when (this) {
    QuoteResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
    QuoteResponse.FUNDS -> PaymentMethodType.FUNDS
    QuoteResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
    QuoteResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
    else -> PaymentMethodType.UNKNOWN
}
