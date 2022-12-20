package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class EmailVerificationModelState(
    val email: String? = null,
    val isVerified: Boolean = false,
    val showResendEmailConfirmation: Boolean = false,
    val error: EmailVerificationError? = null,
) : ModelState

data class EmailVerificationViewState(
    val email: String?,
    val isVerified: Boolean,
    val showResendEmailConfirmation: Boolean,
    val error: EmailVerificationError?,
) : ViewState

sealed class EmailVerificationError {
    data class Generic(val message: String?) : EmailVerificationError()

    object TooManyResendAttempts : EmailVerificationError()
}
