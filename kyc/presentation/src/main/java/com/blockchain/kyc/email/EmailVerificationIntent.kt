package com.blockchain.kyc.email

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface EmailVerificationIntent : Intent<EmailVerificationModelState> {
    object ResendEmailClicked : EmailVerificationIntent
}
