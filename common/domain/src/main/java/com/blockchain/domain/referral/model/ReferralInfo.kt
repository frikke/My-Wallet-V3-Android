package com.blockchain.domain.referral.model

sealed class ReferralInfo {
    data class Data(
        val rewardTitle: String,
        val rewardSubtitle: String,
        val code: String,
        val campaignId: String,
        val criteria: List<String>
    ) : ReferralInfo()
    object NotAvailable : ReferralInfo()
}
