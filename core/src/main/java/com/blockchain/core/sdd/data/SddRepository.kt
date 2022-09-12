package com.blockchain.core.sdd.data

import com.blockchain.core.sdd.data.datasources.SddEligibilityStore
import com.blockchain.core.sdd.domain.SddService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.store.mapData
import kotlinx.coroutines.flow.Flow

internal class SddRepository(
    val sddEligibilityStore: SddEligibilityStore
) : SddService {
    override fun isEligible(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Boolean>> {
        return sddEligibilityStore.stream(freshnessStrategy)
            .mapData { sddEligibility ->
                sddEligibility.eligible && sddEligibility.tier == SDD_ELIGIBLE_TIER
            }
    }

    companion object {
        private const val SDD_ELIGIBLE_TIER = 3
    }
}
