package piuk.blockchain.android.ui.settings.v2

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class ProfileIntent : MviIntent<ProfileState> {

    class UpdateProfileView(
        private val profileViewToLaunch: ProfileViewToLaunch
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                profileViewToLaunch = profileViewToLaunch
            )
    }

    object ProfileUpdatedSuccessfully : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState = oldState
    }

    class UpdateProfile(
        val email: String,
        val phoneNumber: String
    ) : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                email = email,
                phone = phoneNumber,
            )
    }

    object ProfileUpdateFailed : ProfileIntent() {
        override fun reduce(oldState: ProfileState): ProfileState =
            oldState.copy(
                hasFailed = true
            )
    }
}
