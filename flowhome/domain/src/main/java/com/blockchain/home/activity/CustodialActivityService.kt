package com.blockchain.home.activity

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialTransaction
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface CustodialActivityService {
    val defFreshness
        get() = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )

    fun getAllActivity(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<List<CustodialTransaction>>>

    fun getActivity(
        id: String,
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<ActivitySummaryItem>>
}
