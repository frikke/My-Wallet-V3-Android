package piuk.blockchain.android.ui.settings.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviState

data class ProfileState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val error: ProfileError = ProfileError.None,
    val isLoading: Boolean = false,
    val isVerificationSent: VerificationSent? = null
) : MviState

data class VerificationSent(
    val emailSent: Boolean = false,
    val codeSent: Boolean = false
)

enum class ProfileError {
    LoadProfileError,
    SaveEmailError,
    ResendEmailError,
    SavePhoneError,
    ResendSmsError,
    PhoneNumberNotValidError,
    None
}
