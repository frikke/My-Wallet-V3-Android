package com.blockchain.home.presentation.referral

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.domain.referral.model.ReferralInfo

data class ReferralModelState(
    val referralInfo: DataResource<ReferralInfo> = DataResource.Loading,
    val codeCopied: Boolean = false
) : ModelState
