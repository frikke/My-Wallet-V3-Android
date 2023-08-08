package com.blockchain.domain.referral

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.outcome.Outcome
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface ReferralService {
    fun fetchReferralData(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(1, TimeUnit.HOURS)
        )
    ): Flow<DataResource<ReferralInfo>>

    suspend fun isReferralCodeValid(code: String): Outcome<Throwable, Boolean>

    suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit>
}
