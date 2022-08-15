package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.common.model.PromotionStyleInfo

data class ReferralModelState(
    val rewardTitle: String = "",
    val rewardSubtitle: String = "",
    val code: String = "",
    val criteria: List<String> = listOf(),
    val confirmCopiedToClipboard: Boolean = false,
    val promotionStyleInfo: PromotionStyleInfo? = null
) : ModelState
