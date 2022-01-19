package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviState

data class ProfileState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val savingHasFailed: Boolean = false,
    val isLoading: Boolean = false,
    val isVerificationSent: VerificationSent? = null
) : MviState

data class VerificationSent(
    val emailSent: Boolean = false,
    val codeSent: Boolean = false
)
