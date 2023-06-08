package com.blockchain.core.buy.domain

import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.datamanagers.BuyOrderList
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.FiatTransaction
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface SimpleBuyService {

    val defFreshness
        get() = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )

    fun getEligibility(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<SimpleBuyEligibility>>

    fun isEligible(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<Boolean>>

    fun getPairs(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<SimpleBuyPair>>>

    fun getSupportedBuySellCryptoCurrencies(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<CurrencyPair>>>

    fun getBuyOrders(
        freshnessStrategy: FreshnessStrategy = defFreshness,
        pendingOnly: Boolean = false,
        shouldFilterInvalid: Boolean = false
    ): Flow<DataResource<BuyOrderList>>

    fun swapOrders(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<CustodialOrder>>>

    fun getFiatTransactions(
        freshnessStrategy: FreshnessStrategy = defFreshness,
        fiatCurrency: FiatCurrency,
        product: Product,
        type: String? = null
    ): Flow<DataResource<List<FiatTransaction>>>

    fun shouldShowUpsellBuy(): Boolean

    fun dismissUpsellBuy()
}
