package piuk.blockchain.android.ui.settings.v2

import piuk.blockchain.android.ui.base.mvi.MviState

data class ProfileState(
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hasFailed: Boolean = false,
    val profileViewToLaunch: ProfileViewToLaunch = ProfileViewToLaunch.View
) : MviState

sealed class ProfileViewToLaunch {
    object View : ProfileViewToLaunch()
    object Edit : ProfileViewToLaunch()
}
