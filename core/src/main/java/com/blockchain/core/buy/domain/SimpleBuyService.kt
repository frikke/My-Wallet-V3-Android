package com.blockchain.core.buy.domain

import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.datamanagers.BuyOrderList
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.FiatTransaction
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.Flow

interface SimpleBuyService {
    fun getEligibility(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<SimpleBuyEligibility>>

    fun isEligible(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Boolean>>

    fun getPairs(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<List<SimpleBuyPair>>>

    fun getSupportedBuySellCryptoCurrencies(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<CurrencyPair>>>

    fun getBuyOrders(
        pendingOnly: Boolean = false,
        shouldFilterInvalid: Boolean = false
    ): Flow<DataResource<BuyOrderList>>

    fun swapOrders(): Flow<DataResource<List<CustodialOrder>>>

    fun getFiatTransactions(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh),
        fiatCurrency: FiatCurrency,
        product: Product,
        type: String? = null
    ): Flow<DataResource<List<FiatTransaction>>>
}
