package com.blockchain.unifiedcryptowallet.data.activity.datasource

import activity.ActivityItem
import activity.ActivityQueries
import com.blockchain.api.selfcustody.activity.ActivityResponse
import com.blockchain.api.services.ActivityCacheService
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UnifiedActivityCache(
    private val activityQueries: ActivityQueries,
    private val json: Json
) : ActivityCacheService {

    fun getActivity(): Flow<List<ActivityItem>> {
        return activityQueries.selectAllActivity()
            .asFlow()
            .map {
                it.executeAsList()
            }
    }

    fun getActivity(txId: String): Flow<ActivityItem?> {
        return activityQueries.selectByTxId(txId)
            .asFlow()
            .map {
                it.executeAsOneOrNull()
            }
    }

    override fun addOrUpdateActivityItems(items: ActivityResponse) {
        items.activityData.activity.map { activityItem ->
            activityQueries.insert(
                ActivityItem(
                    tx_id = activityItem.id,
                    network = items.activityData.network,
                    pubkey = items.activityData.pubKey,
                    external_url = activityItem.externalUrl,
                    summary_view = json.encodeToString(activityItem.summary),
                    status = activityItem.status,
                    timestamp = activityItem.timestamp ?: 0,
                    last_fetched = 0
                )
            )
        }
    }
}
