package com.blockchain.core.buy.domain

import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.datamanagers.BuyOrderList
import com.blockchain.nabu.datamanagers.CustodialOrder
import kotlinx.coroutines.flow.Flow

interface SimpleBuyService {

    fun getEligibility(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<SimpleBuyEligibility>>

    fun isEligible(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>

    fun getPairs(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Flow<DataResource<List<SimpleBuyPair>>>

    fun getBuyOrders(
        pendingOnly: Boolean = false,
        shouldFilterInvalid: Boolean = false
    ): Flow<DataResource<BuyOrderList>>

    fun swapOrders(): Flow<DataResource<List<CustodialOrder>>>
}
