package com.blockchain.domain.referral

import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.domain.referral.model.ReferralValidity
import com.blockchain.outcome.Outcome

interface ReferralService {

    suspend fun fetchReferralData(): Outcome<Throwable, ReferralInfo>

    suspend fun validateReferralCode(code: String): Outcome<Throwable, ReferralValidity>

    suspend fun associateReferralCodeIfPresent(validatedCode: String?): Outcome<Throwable, Unit>
}
