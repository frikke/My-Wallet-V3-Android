package piuk.blockchain.android.ui.settings.profile.email

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviIntent
import piuk.blockchain.androidcore.data.settings.Email

sealed class EmailIntent : MviIntent<EmailState> {

    object FetchProfile : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.None,
                isLoading = true
            )
    }

    object LoadProfile : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.None,
                isLoading = true
            )
    }

    object LoadProfileFailed : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.LoadProfileError,
                isLoading = false
            )
    }

    class LoadProfileSucceeded(
        private val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                userInfoSettings = userInfoSettings,
                isLoading = false
            )
    }

    object InvalidateCache : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState = oldState
    }

    class SaveEmail(
        val email: String
    ) : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.None,
                isLoading = true
            )
    }

    class SaveEmailSucceeded(
        val settings: Email
    ) : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                isLoading = false,
                emailSent = true,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = settings.address,
                    emailVerified = settings.isVerified,
                    mobileWithPrefix = oldState.userInfoSettings?.mobileWithPrefix,
                    mobileVerified = oldState.userInfoSettings?.mobileVerified ?: false,
                    smsDialCode = oldState.userInfoSettings?.smsDialCode.orEmpty()
                )
            )
    }

    object SaveEmailFailed : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.SaveEmailError,
                isLoading = false
            )
    }

    object ResendEmail : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.None,
                isLoading = true,
                emailSent = false
            )
    }

    object ResendEmailFailed : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.ResendEmailError,
                isLoading = false,
                emailSent = false
            )
    }

    data class ResendEmailSucceeded(val email: Email) : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                isLoading = false,
                emailSent = true,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = email.address,
                    emailVerified = email.isVerified,
                    mobileWithPrefix = oldState.userInfoSettings?.mobileWithPrefix,
                    mobileVerified = oldState.userInfoSettings?.mobileVerified ?: false,
                    smsDialCode = oldState.userInfoSettings?.smsDialCode.orEmpty()
                )
            )
    }

    object ResetEmailSentVerification : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                emailSent = false
            )
    }

    object ClearErrors : EmailIntent() {
        override fun reduce(oldState: EmailState): EmailState =
            oldState.copy(
                error = EmailError.None
            )
    }
}
