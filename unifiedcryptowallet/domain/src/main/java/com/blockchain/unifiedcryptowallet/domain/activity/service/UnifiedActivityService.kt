package com.blockchain.unifiedcryptowallet.domain.activity.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage
import kotlinx.coroutines.flow.Flow

interface UnifiedActivityService {
    suspend fun activityForAccount(
        pubKey: String,
        currency: String,
        acceptLanguage: String,
        timeZone: String,
        nextPage: String? = null,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<UnifiedActivityPage>>
}
