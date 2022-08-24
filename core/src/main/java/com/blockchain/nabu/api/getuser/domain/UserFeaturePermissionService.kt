package com.blockchain.nabu.api.getuser.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import kotlinx.coroutines.flow.Flow

interface UserFeaturePermissionService {
    fun isEligibleFor(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>

    fun getAccessForFeature(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<FeatureAccess>>
}
