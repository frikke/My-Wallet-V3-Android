package piuk.blockchain.android.ui.settings.v2.sheets.sms

import com.blockchain.commonarch.presentation.mvi.MviState

data class SMSVerificationState(
    val error: VerificationError? = null,
    val isLoading: Boolean = false,
    val isCodeSmsSent: Boolean = false,
    val isPhoneVerified: Boolean = false
) : MviState

enum class VerificationError {
    VerifyPhoneError,
    ResendSmsError
}
