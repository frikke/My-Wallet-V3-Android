package com.dex.domain

import com.blockchain.data.DataResource
import com.blockchain.nabu.FeatureAccess
import kotlinx.coroutines.flow.Flow

interface DexEligibilityService {
    fun dexEligibility(): Flow<DataResource<FeatureAccess>>
}
