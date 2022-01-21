package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class SMSVerificationIntent : MviIntent<SMSVerificationState> {

    data class ResendCodeSMS(val phoneNumber: String) : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                isLoading = true
            )
    }

    object ResendCodeSMSFailed : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                error = VerificationError.ResendSmsError,
                isLoading = false
            )
    }

    object ResendCodeSMSSucceeded : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                isLoading = false,
                isCodeSmsSent = true
            )
    }

    object ResetCodeSentVerification : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                isCodeSmsSent = false
            )
    }

    data class VerifyPhoneNumber(val code: String) : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                isLoading = true
            )
    }

    object VerifyPhoneNumberFailed : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                error = VerificationError.VerifyPhoneError,
                isLoading = false
            )
    }

    object VerifyPhoneNumberSucceeded : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                isLoading = false,
                isPhoneVerified = true
            )
    }

    object ClearErrors : SMSVerificationIntent() {
        override fun reduce(oldState: SMSVerificationState): SMSVerificationState =
            oldState.copy(
                error = null
            )
    }
}
