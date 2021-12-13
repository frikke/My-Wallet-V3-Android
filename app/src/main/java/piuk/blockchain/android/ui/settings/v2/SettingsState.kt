package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviState

data class SettingsState(
    val userInformation: BasicProfileInfo? = null,
    val contactSupportLoaded: Boolean = false
) : MviState
