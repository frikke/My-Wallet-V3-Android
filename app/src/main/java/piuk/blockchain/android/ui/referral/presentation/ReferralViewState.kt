package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class ReferralViewState(
    val code: String,
    val criteria: List<String>,
    val rewardSubtitle: String,
    val rewardTitle: String,
    val confirmCopiedToClipboard: Boolean,
    val promotionData: ReferralPromotionStyleInfo?
) : ViewState
