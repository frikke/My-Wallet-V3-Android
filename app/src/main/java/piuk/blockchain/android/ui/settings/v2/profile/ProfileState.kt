package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviState

data class ProfileState(
    val userInfoSettings: WalletSettingsService.UserInfoSettings? = null,
    val basicProfileInfo: BasicProfileInfo? = null,
    val loadingHasFailed: Boolean = false,
    val savingHasFailed: Boolean = false,
    val profileViewState: ProfileViewState = ProfileViewState.View
) : MviState

sealed class ProfileViewState {
    object View : ProfileViewState()
    object Edit : ProfileViewState()
}
