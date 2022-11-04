package com.blockchain.unifiedcryptowallet.data.activity.repository

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.data.activity.datasource.UnifiedActivityStore
import com.blockchain.unifiedcryptowallet.data.activity.repository.mapper.toActivityDetailGroups
import com.blockchain.unifiedcryptowallet.data.activity.repository.mapper.toActivityViewItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import kotlinx.coroutines.flow.Flow

class UnifiedActivityRepository(
    private val unifiedActivityStore: UnifiedActivityStore
) : UnifiedActivityService {

    override suspend fun activityForAccount(
        pubKey: String,
        currency: String,
        acceptLanguage: String,
        timeZone: String,
        nextPage: String?,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<UnifiedActivityPage>> {

        return unifiedActivityStore.stream(
            freshnessStrategy.withKey(
                UnifiedActivityStore.Key(
                    currency,
                    pubKey,
                    acceptLanguage,
                    timeZone,
                    nextPage
                )
            )
        ).mapData { activityResponse ->
            UnifiedActivityPage(
                activity = activityResponse.activity.mapNotNull { activityItem ->
                    val summary = activityItem.summary.toActivityViewItem()
                    val detail = activityItem.detail.toActivityDetailGroups()

                    if (summary == null || detail == null) {
                        null
                    } else {
                        UnifiedActivityItem(
                            txId = activityItem.id,
                            blockExplorerUrl = activityItem.externalUrl,
                            summary = summary,
                            detail = detail,
                            status = activityItem.status,
                            timestamp = activityItem.timestamp
                        )
                    }
                },
                nextPage = activityResponse.nextPage
            )
        }
    }
}
