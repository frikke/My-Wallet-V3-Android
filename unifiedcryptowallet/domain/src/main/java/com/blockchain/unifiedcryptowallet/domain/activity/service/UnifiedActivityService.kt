package com.blockchain.unifiedcryptowallet.domain.activity.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import kotlinx.coroutines.flow.Flow

interface UnifiedActivityService {
    fun getAllActivity(
        acceptLanguage: String,
        timeZone: String
    ): Flow<DataResource<List<UnifiedActivityItem>>>

    suspend fun getActivityDetails(
        txId: String,
        network: String,
        pubKey: String,
        locales: String,
        timeZone: String,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<ActivityDetailGroups>>
}
