package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SettingsIntent : MviIntent<SettingsState> {

    object LoadContactSupportEligibility : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState = oldState
    }

    class LoadedContactSupportEligibility(
        private val contactSupportLoaded: Boolean,
        private val userInformation: BasicProfileInfo? = null
    ) : SettingsIntent() {
        override fun reduce(oldState: SettingsState): SettingsState =
            oldState.copy(
                contactSupportLoaded = contactSupportLoaded,
                userInformation = userInformation
            )
    }
}
