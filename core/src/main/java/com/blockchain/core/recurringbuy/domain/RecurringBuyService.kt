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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface RecurringBuyService {
    suspend fun isEligible(): Boolean

    fun recurringBuys(
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<RecurringBuy>>>

    fun recurringBuys(
        asset: AssetInfo,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<RecurringBuy>>>

    fun recurringBuy(
        id: String,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<RecurringBuy>>

    suspend fun cancelRecurringBuy(recurringBuy: RecurringBuy)

    fun frequencyConfig(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<List<EligibleAndNextPaymentRecurringBuy>>>

    suspend fun createOrder(
        request: RecurringBuyRequest
    ): Outcome<Exception, RecurringBuyOrder>
}
