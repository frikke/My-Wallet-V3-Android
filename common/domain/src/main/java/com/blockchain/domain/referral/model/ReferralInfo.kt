package com.blockchain.domain.referral.model

import com.blockchain.domain.common.model.PromotionStyleInfo

sealed class ReferralInfo {
    data class Data(
        val rewardTitle: String,
        val rewardSubtitle: String,
        val code: String,
        val campaignId: String,
        val criteria: List<String>,
        val announcementInfo: PromotionStyleInfo?,
        val promotionInfo: PromotionStyleInfo?
    ) : ReferralInfo()

    object NotAvailable : ReferralInfo()
}
