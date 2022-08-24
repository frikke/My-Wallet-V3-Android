package com.blockchain.core.sdd.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import kotlinx.coroutines.flow.Flow

/**
 * Simplified Due Diligence Operations
 */
interface SddService {
    fun isEligible(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>
}
