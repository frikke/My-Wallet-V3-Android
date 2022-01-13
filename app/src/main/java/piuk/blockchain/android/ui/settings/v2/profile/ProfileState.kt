package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviState

data class ProfileState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val basicProfileInfo: BasicProfileInfo? = null,
    val savingHasFailed: Boolean = false,
    val isLoading: Boolean = false,
    val isVerificationSent: VerificationSent? = null,
    val profileViewState: ProfileViewState = ProfileViewState.View
) : MviState

data class VerificationSent(
    val emailSent: Boolean = false,
    val codeSent: Boolean = false
)

sealed class ProfileViewState {
    object View : ProfileViewState()
    object Edit : ProfileViewState()
}
