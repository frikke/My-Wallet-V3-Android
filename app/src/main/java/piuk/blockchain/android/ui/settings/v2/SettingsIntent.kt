package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SettingsIntent : MviIntent<SettingsState> {
    object LoadInitialInformation : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState
    }

    class UpdateContactSupportEligibility(
        private val isSupportChatEnabled: Boolean,
        private val userInformation: BasicProfileInfo? = null
    ) : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState =
            oldState.copy(
                isSupportChatEnabled = isSupportChatEnabled,
                basicProfileInfo = userInformation
            )
    }

    object UnpairWallet : SettingsIntent() {
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
