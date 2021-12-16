package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.ui.base.mvi.MviState

data class SettingsState(
    val basicProfileInfo: BasicProfileInfo? = null,
    val hasWalletUnpaired: Boolean = false,
    val isSupportChatEnabled: Boolean = false,
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    object Profile : ViewToLaunch()
}
