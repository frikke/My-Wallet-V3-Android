package piuk.blockchain.android.ui.settings.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.mvi.MviIntent

sealed class ProfileIntent : MviIntent<ProfileState> {

    object FetchProfile : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                error = ProfileError.None,
                isLoading = true
            )
    }

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
}
