package com.blockchain.domain.trade

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.trade.model.RecurringBuy
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface TradeDataService {

    fun isFirstTimeBuyer(): Single<Boolean>

    fun getEligibilityAndNextPaymentDate(): Single<List<EligibleAndNextPaymentRecurringBuy>>

    fun getRecurringBuysForAsset(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<RecurringBuy>>>

    fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy>

    fun cancelRecurringBuy(recurringBuy: RecurringBuy): Completable

    suspend fun getBuyQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        paymentMethod: PaymentMethodType,
    ): Outcome<Exception, QuotePrice>

    suspend fun getSellQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Outcome<Exception, QuotePrice>

    suspend fun getSwapQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Outcome<Exception, QuotePrice>
}
