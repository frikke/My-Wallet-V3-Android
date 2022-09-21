package com.blockchain.api.services

import com.blockchain.api.referral.ReferralApi
import com.blockchain.api.referral.data.ReferralCode

class ReferralApiService(private val api: ReferralApi) {

    suspend fun getReferralCode(
        currency: String
    ) = api.getReferralCode(
        currency = currency,
        platform = WALLET
    )

    suspend fun validateReferralCode(
        referralCode: String
    ) = api.validateReferralCode(referralCode)

    suspend fun associateReferralCode(
        referralCode: String
    ) = api.associateReferral(ReferralCode(referralCode))
}

private const val WALLET = "wallet"
