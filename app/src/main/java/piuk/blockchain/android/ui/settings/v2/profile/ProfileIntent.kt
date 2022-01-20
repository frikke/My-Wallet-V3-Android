package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.wallet.api.data.Settings
import piuk.blockchain.androidcore.data.settings.Email

sealed class ProfileIntent : MviIntent<ProfileState> {

    object LoadProfile : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None,
                isLoading = true
            )
    }

    object LoadProfileFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.LoadProfileError,
                isLoading = false
            )
    }

    class LoadProfileSucceeded(
        private val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                userInfoSettings = userInfoSettings,
                isLoading = false
            )
    }

    class SaveEmail(
        val email: String
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None,
                isLoading = true
            )
    }

    class SaveEmailSucceeded(
        val settings: Settings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = settings.email,
                    emailVerified = settings.isEmailVerified,
                    mobileWithPrefix = oldState.userInfoSettings?.mobileWithPrefix,
                    mobileVerified = oldState.userInfoSettings?.mobileVerified ?: false
                )
            )
    }

    object SaveEmailFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.SaveEmailError,
                isLoading = false
            )
    }

    object ResendEmail : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None,
                isLoading = true,
                isVerificationSent = VerificationSent(emailSent = false)
            )
    }

    object ResendEmailFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.ResendEmailError,
                isLoading = false,
                isVerificationSent = VerificationSent(emailSent = false)
            )
    }

    data class ResendEmailSucceeded(val email: Email) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                isVerificationSent = VerificationSent(
                    codeSent = oldState.isVerificationSent?.codeSent ?: false,
                    emailSent = true
                ),
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = email.address,
                    emailVerified = email.isVerified,
                    mobileWithPrefix = oldState.userInfoSettings?.mobileWithPrefix,
                    mobileVerified = oldState.userInfoSettings?.mobileVerified ?: false
                )
            )
    }

    class SavePhoneNumber(val phoneNumber: String) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None,
                isLoading = true
            )
    }

    class SavePhoneNumberSucceeded(
        val settings: Settings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = oldState.userInfoSettings?.email,
                    emailVerified = oldState.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified
                ),
                isVerificationSent = VerificationSent(
                    codeSent = true,
                    emailSent = oldState.isVerificationSent?.emailSent ?: false,
                )
            )
    }

    object SavePhoneNumberFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.SavePhoneError,
                isLoading = false
            )
    }

    object ResendCodeSMS : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true
            )
    }

    object ResendCodeSMSFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.ResendSmsError,
                isLoading = false
            )
    }

    data class ResendCodeSMSSucceeded(val settings: Settings) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = oldState.userInfoSettings?.email,
                    emailVerified = oldState.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified
                ),
                isVerificationSent = VerificationSent(
                    codeSent = true,
                    emailSent = oldState.isVerificationSent?.emailSent ?: false,
                )
            )
    }

    object ResetEmailSentVerification : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isVerificationSent = VerificationSent(
                    codeSent = oldState.isVerificationSent?.codeSent ?: false,
                    emailSent = false
                )
            )
    }

    object ResetCodeSentVerification : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isVerificationSent = VerificationSent(
                    codeSent = false,
                    emailSent = oldState.isVerificationSent?.emailSent ?: false
                )
            )
    }

    object ClearErrors : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None
            )
    }

    data class VerifyPhoneNumber(val code: String) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true
            )
    }

    object VerifyPhoneNumberFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.VerifyPhoneError,
                isLoading = false
            )
    }
}
