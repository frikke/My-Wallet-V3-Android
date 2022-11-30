package com.blockchain.home.presentation.referral

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.domain.referral.model.ReferralInfo

data class ReferralViewState(
    val referralInfo: DataResource<ReferralInfo>
) : ViewState
