package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ProfileIntent : MviIntent<ProfileState> {

    class UpdateProfileView(
        private val profileViewToLaunch: ProfileViewState
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                profileViewState = profileViewToLaunch
            )
    }

    object LoadProfile : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState = oldState
    }

    class ProfileInfoLoaded(
        private val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                userInfoSettings = userInfoSettings,
                loadingHasFailed = false
            )
    }

    object ProfileInfoFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                loadingHasFailed = true
            )
    }

    class UpdateProfile(
        val userInfoSettings: WalletSettingsService.UserInfoSettings
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                userInfoSettings = userInfoSettings
            )
    }

    object ProfileUpdatedSuccessfully : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                savingHasFailed = false
            )
    }

    object ProfileUpdateFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                savingHasFailed = true
            )
    }
}
