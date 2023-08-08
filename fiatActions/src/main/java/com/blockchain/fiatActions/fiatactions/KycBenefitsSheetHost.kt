package com.blockchain.fiatActions.fiatactions

import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog

interface KycBenefitsSheetHost : SlidingModalBottomDialog.Host {
    fun verificationCtaClicked()
}
