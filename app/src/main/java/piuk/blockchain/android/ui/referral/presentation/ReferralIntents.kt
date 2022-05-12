package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ReferralIntents : Intent<ReferralModelState> {
    object ConfirmCopiedToClipboard : ReferralIntents
}
