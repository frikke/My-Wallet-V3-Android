package com.blockchain.home.activity

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import kotlinx.coroutines.flow.Flow

interface CustodialActivityService {
    fun getAllActivity(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<List<ActivitySummaryItem>>>

    fun getActivity(
        txId: String,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<ActivitySummaryItem>>
}
