package com.blockchain.core.sdd.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import kotlinx.coroutines.flow.Flow

/**
 * Simplified Due Diligence Operations
 */
interface SddService {
    fun isEligible(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Boolean>>
}
