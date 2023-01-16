package com.blockchain.domain.referral

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface ReferralService {
    val defFreshness
        get() = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(1, TimeUnit.MINUTES)
        )

    fun fetchReferralData(
        freshnessStrategy: FreshnessStrategy = defFreshness
    ): Flow<DataResource<ReferralInfo>>

    suspend fun isReferralCodeValid(code: String): Outcome<Throwable, Boolean>

    suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit>
}
