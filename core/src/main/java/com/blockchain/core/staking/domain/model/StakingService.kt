package com.blockchain.core.staking.domain.model

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

interface StakingService {

    fun getEligibilityForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>>

    fun getActiveAssets(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<Set<AssetInfo>>

    suspend fun getRateForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Double>>
}
