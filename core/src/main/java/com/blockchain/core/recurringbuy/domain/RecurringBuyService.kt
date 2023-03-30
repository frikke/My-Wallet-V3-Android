package com.blockchain.core.recurringbuy.domain

import com.blockchain.core.recurringbuy.domain.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyOrder
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyRequest
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

interface RecurringBuyService {
    fun recurringBuys(
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<RecurringBuy>>>

    fun recurringBuys(
        asset: AssetInfo,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<RecurringBuy>>>

    fun recurringBuy(
        id: String,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<RecurringBuy>>

    suspend fun cancelRecurringBuy(recurringBuy: RecurringBuy)

    fun frequencyConfig(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<List<EligibleAndNextPaymentRecurringBuy>>>

    suspend fun createOrder(
        request: RecurringBuyRequest
    ): Outcome<Exception, RecurringBuyOrder>
}
