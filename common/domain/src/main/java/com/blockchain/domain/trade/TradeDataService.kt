package com.blockchain.domain.trade

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.trade.model.RecurringBuy
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
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

    fun getQuotePrice(
        currencyPair: String,
        amount: String,
        paymentMethod: String,
        orderProfileName: String
    ): Observable<QuotePrice>
}
