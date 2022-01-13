package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import info.blockchain.wallet.api.data.Settings
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.androidcore.data.settings.Email

sealed class ProfileIntent : MviIntent<ProfileState> {

    class UpdateProfileView(
        private val profileViewToLaunch: ProfileViewState
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                profileViewState = profileViewToLaunch,
                savingHasFailed = false
            )
    }

    object LoadProfile : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true
            )
    }

    object LoadProfileFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
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

    class SaveProfile(
        val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true
            )
    }

    class SaveProfileSucceeded(
        val email: Email,
        val settings: Settings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                savingHasFailed = false,
                profileViewState = ProfileViewState.View,
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = email.address,
                    emailVerified = email.isVerified,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified
                )
            )
    }

    object SaveProfileFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                savingHasFailed = true,
                isLoading = false
            )
    }

    data class SaveAndSendEmail(val email: String) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true,
                isVerificationSent = VerificationSent(emailSent = false)
            )
    }

    object SaveAndSendEmailFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                isVerificationSent = VerificationSent(emailSent = false)
            )
    }

    object SaveAndSendEmailSucceeded : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
                isVerificationSent = VerificationSent(
                    codeSent = oldState.isVerificationSent?.codeSent ?: false,
                    emailSent = true
                )
            )
    }

    data class SaveAndSendSMS(val mobileWithPrefix: String) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = true
            )
    }

    object SaveAndSendSMSFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false
            )
    }

    object SaveAndSendSMSSucceeded : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                isLoading = false,
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

    data class VerifyPhoneNumber(val code: String) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(isLoading = true)
    }

    object VerifyPhoneNumberFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(isLoading = false)
    }
}
