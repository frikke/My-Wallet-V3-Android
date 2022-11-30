package com.blockchain.home.presentation.referral

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class ReferralIntent : Intent<ReferralModelState> {
    object LoadData : ReferralIntent()
}
