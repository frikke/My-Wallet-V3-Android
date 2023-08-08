package com.blockchain.unifiedcryptowallet.domain.activity.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import kotlinx.coroutines.flow.Flow

interface UnifiedActivityService {
    fun getAllActivity(): Flow<DataResource<List<UnifiedActivityItem>>>

    fun getActivity(
        txId: String
    ): Flow<DataResource<UnifiedActivityItem>>

    fun getActivityDetails(
        txId: String,
        network: String,
        pubKey: String,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<ActivityDetailGroups>>

    fun clear()
}
