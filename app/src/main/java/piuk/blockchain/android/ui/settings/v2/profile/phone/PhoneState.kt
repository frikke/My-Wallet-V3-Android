package piuk.blockchain.android.ui.settings.v2.profile.phone

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviState

data class PhoneState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val error: PhoneError = PhoneError.None,
    val isLoading: Boolean = false,
    val codeSent: Boolean = false
) : MviState

enum class PhoneError {
    LoadProfileError,
    SaveEmailError,
    ResendEmailError,
    SavePhoneError,
    ResendSmsError,
    PhoneNumberNotValidError,
    None
}
