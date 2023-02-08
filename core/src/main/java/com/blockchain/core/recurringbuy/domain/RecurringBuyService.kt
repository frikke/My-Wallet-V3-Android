package com.blockchain.core.recurringbuy.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.trade.model.RecurringBuy
import kotlinx.coroutines.flow.Flow

interface RecurringBuyService {
    fun getRecurringBuyForId(
        id: String,
        includeInactive: Boolean = false,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<RecurringBuy>>

    // todo(othman) add rest of the stuff
}
