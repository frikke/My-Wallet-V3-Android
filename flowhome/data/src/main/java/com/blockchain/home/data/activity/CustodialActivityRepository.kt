package com.blockchain.home.data.activity

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.data.activity.dataresource.CustodialActivityStore
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class CustodialActivityRepository(
    private val custodialActivityStore: CustodialActivityStore
) : CustodialActivityService {
    override fun getAllActivity(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<ActivitySummaryItem>>> {
        return custodialActivityStore.stream(freshnessStrategy)
    }

    override fun getActivity(
        id: String,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<ActivitySummaryItem>> {
        return getAllActivity(freshnessStrategy)
            .mapData { activityList ->
                id.split("|").let { txIdAndClass ->
                    activityList.first { it.txId == txIdAndClass[0] && it::class.toString() == txIdAndClass[1] }
                }
            }
            .catch {
                emit(DataResource.Error(Exception(it)))
            }
    }
}
