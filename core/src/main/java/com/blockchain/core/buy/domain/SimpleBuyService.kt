package com.blockchain.core.buy.domain

import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import kotlinx.coroutines.flow.Flow

interface SimpleBuyService {

    fun getEligibility(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<SimpleBuyEligibility>>

    /**
     * @return true if simple buy is eligible
     */
    fun isEligible(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<Boolean>>
}
