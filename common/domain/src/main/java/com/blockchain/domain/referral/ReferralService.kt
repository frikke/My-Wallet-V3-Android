package com.blockchain.domain.referral

import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.outcome.Outcome

interface ReferralService {

    suspend fun fetchReferralData(): Outcome<Throwable, ReferralInfo>

    suspend fun isReferralCodeValid(code: String): Outcome<Throwable, Boolean>

    suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit>
}
