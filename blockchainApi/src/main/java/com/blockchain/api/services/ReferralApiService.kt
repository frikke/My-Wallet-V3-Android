package com.blockchain.api.services

import com.blockchain.api.referral.ReferralApi

class ReferralApiService(private val api: ReferralApi) {

    suspend fun getReferralCode(
        authorization: String,
        currency: String
    ) = api.getReferralCode(
        authorization = authorization,
        currency = currency,
        platform = WALLET
    )

    suspend fun validateReferralCode(
        referralCode: String
    ) = api.validateReferralCode(referralCode)

    suspend fun associateReferralCode(
        authorization: String,
        referralCode: String
    ) = api.associateReferral(authorization, referralCode)
}

private const val WALLET = "wallet"
