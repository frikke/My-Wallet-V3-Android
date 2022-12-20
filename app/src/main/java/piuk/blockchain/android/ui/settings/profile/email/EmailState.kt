package piuk.blockchain.android.ui.settings.profile.email

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviState

data class EmailState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val error: EmailError = EmailError.None,
    val isLoading: Boolean = false,
    val emailSent: Boolean = false
) : MviState

enum class EmailError {
    LoadProfileError,
    SaveEmailError,
    ResendEmailError,
    ResendSmsError,
    None
}
