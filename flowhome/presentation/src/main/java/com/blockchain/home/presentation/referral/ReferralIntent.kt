package com.blockchain.home.presentation.referral

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.presentation.pulltorefresh.canRefresh

sealed interface ReferralIntent : Intent<ReferralModelState> {
    data class LoadData(
        val forceRefresh: Boolean = false
    ) : ReferralIntent

    object CodeCopied : ReferralIntent

    object RefreshRequested : ReferralIntent {
        override fun isValidFor(modelState: ReferralModelState): Boolean {
            return canRefresh(modelState.lastFreshDataTime)
        }
    }
}
