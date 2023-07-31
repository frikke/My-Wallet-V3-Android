package com.blockchain.home.activity

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialTransaction
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface CustodialActivityService {
    fun getAllActivity(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<CustodialTransaction>>>

    fun getActivity(
        id: String,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<ActivitySummaryItem>>
}
