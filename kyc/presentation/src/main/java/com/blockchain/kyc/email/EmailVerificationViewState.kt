package com.blockchain.kyc.email

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class EmailVerificationViewState(
    val email: String,
    val status: EmailVerificationStatus,
    val showResendingEmailInProgress: Boolean,
    val snackbarMessage: EmailVerificationNotification?
) : ViewState
