package com.blockchain.kyc.email

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class EmailVerificationModelState(
    val email: String? = null,
    val status: EmailVerificationStatus = EmailVerificationStatus.Default,
    val isResendingEmailInProgress: Boolean = false,
    val notification: EmailVerificationNotification? = null
) : ModelState

enum class EmailVerificationStatus {
    Default,
    Error,
    Success
}

sealed class EmailVerificationError {
    data class Generic(val message: String?) : EmailVerificationError()
    object TooManyResendAttempts : EmailVerificationError()
}

sealed interface EmailVerificationNotification {
    object EmailSent : EmailVerificationNotification
    data class Error(val error: EmailVerificationError) : EmailVerificationNotification
}
