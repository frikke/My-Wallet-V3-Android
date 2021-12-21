package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SettingsIntent : MviIntent<SettingsState> {
    object LoadSupportEligibilityAndUserInfo : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState
    }

    class UpdateContactSupportEligibility(
        private val tier: Tier,
        private val userInformation: BasicProfileInfo? = null
    ) : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState =
            oldState.copy(
                basicProfileInfo = userInformation,
                tier = tier
            )
    }

    object LogOut : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState
    }

    object UserLoggedOut : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState =
            oldState.copy(
                hasWalletUnpaired = true
            )
    }

    class UpdateViewToLaunch(private val viewToLaunch: ViewToLaunch) : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState.copy(viewToLaunch = viewToLaunch)
    }

    object ResetViewState : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState.copy(viewToLaunch = ViewToLaunch.None)
    }
}
