package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.commonarch.presentation.mvi.MviIntent
import piuk.blockchain.androidcore.data.settings.Email

sealed class EmailVerificationIntent : MviIntent<EmailVerificationState> {

    class EmailUpdated(private val mail: Email) : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState =
            oldState.copy(
                email = mail,
                isLoading = false,
                emailChanged = oldState.email.address != mail.address &&
                    oldState.email.address.isNotEmpty() &&
                    mail.address.isNotEmpty()
            )
    }

    class UpdateEmailInput(private val emailInput: String) : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState =
            oldState.copy(emailInput = emailInput)
    }

    object ErrorEmailVerification : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState.copy(hasError = true)
    }

    object FetchEmail : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState.copy(isLoading = true)
    }

    object CancelEmailVerification : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState
    }

    object EmailUpdateFailed : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState.copy(isLoading = false)
    }

    object StartEmailVerification : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState
    }

    object UpdateEmail : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState.copy(isLoading = true)
    }

    object ResendEmail : EmailVerificationIntent() {
        override fun reduce(oldState: EmailVerificationState): EmailVerificationState = oldState.copy(isLoading = true)
    }
}
