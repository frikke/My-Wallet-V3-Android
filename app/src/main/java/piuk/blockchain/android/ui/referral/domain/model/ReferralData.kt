package piuk.blockchain.android.ui.referral.domain.model

data class ReferralData(
    val rewardTitle: String,
    val rewardSubtitle: String,
    val code: String,
    val criteria: List<String>
)
