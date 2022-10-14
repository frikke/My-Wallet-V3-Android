package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviIntent
import info.blockchain.wallet.api.data.Settings

sealed class PhoneIntent : MviIntent<PhoneState> {

    object FetchProfile : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.None,
                isLoading = true
            )
    }

    object LoadProfile : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.None,
                isLoading = true
            )
    }

    object LoadProfileFailed : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.LoadProfileError,
                isLoading = false
            )
    }

    class LoadProfileSucceeded(
        private val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                userInfoSettings = userInfoSettings,
                isLoading = false
            )
    }

    class SavePhoneNumber(val phoneNumber: String) : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.None,
                isLoading = true
            )
    }

    object SavePhoneNumberFailed : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.SavePhoneError,
                isLoading = false
            )
    }

    object PhoneNumberNotValid : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.PhoneNumberNotValidError,
                isLoading = false
            )
    }

    object ResendCodeSMS : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                isLoading = true
            )
    }

    object ResendCodeSMSFailed : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.ResendSmsError,
                isLoading = false
            )
    }

    data class ResendCodeSMSSucceeded(val settings: Settings) : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = oldState.userInfoSettings?.email,
                    emailVerified = oldState.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified,
                    smsDialCode = settings.smsDialCode
                ),
                codeSent = true
            )
    }

    class SavePhoneNumberSucceeded(
        val settings: Settings
    ) : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                isLoading = false,
                userInfoSettings = WalletSettingsService.UserInfoSettings(
                    email = oldState.userInfoSettings?.email,
                    emailVerified = oldState.userInfoSettings?.emailVerified ?: false,
                    mobileWithPrefix = settings.smsNumber,
                    mobileVerified = settings.isSmsVerified,
                    smsDialCode = settings.smsDialCode
                ),
                codeSent = true
            )
    }

    object ResetCodeSentVerification : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                codeSent = false
            )
    }

    object ClearErrors : PhoneIntent() {
        override fun reduce(oldState: PhoneState): PhoneState =
            oldState.copy(
                error = PhoneError.None
            )
    }
}
