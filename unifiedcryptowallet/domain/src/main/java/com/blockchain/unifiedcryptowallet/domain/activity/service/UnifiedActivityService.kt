package com.blockchain.unifiedcryptowallet.domain.activity.service

import com.blockchain.data.DataResource
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import kotlinx.coroutines.flow.Flow

interface UnifiedActivityService {
    suspend fun getAllActivity(
        acceptLanguage: String,
        timeZone: String
    ): Flow<DataResource<List<UnifiedActivityItem>>>
}
