package piuk.blockchain.android.data

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.api.trade.data.QuoteResponse
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import piuk.blockchain.android.domain.repositories.TradeDataService

class TradeDataRepository(
    private val tradeService: TradeService,
    private val accumulatedInPeriodMapper: Mapper<List<AccumulatedInPeriod>, Boolean>,
    private val nextPaymentRecurringBuyMapper:
        Mapper<List<NextPaymentRecurringBuy>, List<EligibleAndNextPaymentRecurringBuy>>,
    private val recurringBuyMapper: Mapper<List<RecurringBuyResponse>, List<RecurringBuy>>,
    private val getRecurringBuysStore: GetRecurringBuysStore,
    private val assetCatalogue: AssetCatalogue
) : TradeDataService {

    override fun isFirstTimeBuyer(): Single<Boolean> =
        tradeService.isFirstTimeBuyer()
            .map {
                accumulatedInPeriodMapper.map(it.tradesAccumulated)
            }

    override fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>> =
        tradeService.getNextPaymentDate()
            .map {
                nextPaymentRecurringBuyMapper.map(it.nextPayments)
            }

    override fun getRecurringBuysForAsset(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<RecurringBuy>>> {
        return getRecurringBuysStore
            .stream(
                freshnessStrategy.withKey(GetRecurringBuysStore.Key(asset.networkTicker))
            )
            .mapData {
                recurringBuyMapper.map(it)
            }
    }

    override fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy> =
        tradeService.getRecurringBuyForId(recurringBuyId)
            .map {
                recurringBuyMapper.map(it).first()
            }

    override fun cancelRecurringBuy(recurringBuyId: String): Completable =
        tradeService.cancelRecurringBuy(recurringBuyId)

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
    val resultAmount: Money,
    val dynamicFee: Money,
    val networkFee: Money?,
    val paymentMethod: PaymentMethodType,
    val orderProfileName: String
)

private fun String.toPaymentMethodType(): PaymentMethodType = when (this) {
    QuoteResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
    QuoteResponse.FUNDS -> PaymentMethodType.FUNDS
    QuoteResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
    QuoteResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
    else -> PaymentMethodType.UNKNOWN
}
