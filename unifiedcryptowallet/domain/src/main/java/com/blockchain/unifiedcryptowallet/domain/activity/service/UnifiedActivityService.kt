package com.blockchain.unifiedcryptowallet.domain.activity.service

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import kotlinx.coroutines.flow.Flow

interface UnifiedActivityService {
    fun getAllActivity(
        acceptLanguage: String,
        timeZone: String
    ): Flow<DataResource<List<UnifiedActivityItem>>>

    fun getActivity(
        txId: String
    ): Flow<DataResource<UnifiedActivityItem>>

    fun getActivityDetails(
        txId: String,
        network: String,
        pubKey: String,
        pubKeyStyle: PubKeyStyle,
        pubKeyDescriptor: String,
        locales: String,
        timeZone: String,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<ActivityDetailGroups>>

    fun clearCache()
}
