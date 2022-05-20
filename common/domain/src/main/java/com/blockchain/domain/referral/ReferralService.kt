package com.blockchain.domain.referral

import com.blockchain.domain.referral.model.ReferralInfo

interface ReferralService {

    suspend fun fetchReferralData(): ReferralInfo
}
