package piuk.blockchain.android.ui.home.models

import com.blockchain.domain.referral.model.ReferralInfo

data class ReferralState(
    val referralInfo: ReferralInfo,
    val hasReferralBeenClicked: Boolean = false
)
