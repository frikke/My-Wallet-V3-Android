package com.blockchain.core.recurringbuy.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import kotlinx.coroutines.flow.Flow

interface RecurringBuyService {
    fun getRecurringBuyForId(
        id: String,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<RecurringBuy>>

    // todo(othman) add rest of the stuff
}
