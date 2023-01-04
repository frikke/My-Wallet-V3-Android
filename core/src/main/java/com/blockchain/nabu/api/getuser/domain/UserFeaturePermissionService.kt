package com.blockchain.nabu.api.getuser.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import kotlinx.coroutines.flow.Flow

interface UserFeaturePermissionService {
    fun isEligibleFor(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Boolean>>

    fun getAccessForFeature(
        feature: Feature,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<FeatureAccess>>

    fun getAccessForFeatures(
        vararg features: Feature,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Map<Feature, FeatureAccess>>>
}
