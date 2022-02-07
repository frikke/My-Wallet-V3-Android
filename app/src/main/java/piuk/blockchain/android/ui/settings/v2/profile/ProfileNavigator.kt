package piuk.blockchain.android.ui.settings.v2.profile

import piuk.blockchain.android.ui.base.FlowFragment

interface ProfileNavigatorScreen : FlowFragment {
    fun navigator(): ProfileNavigator
}

interface ProfileNavigator {
    fun goToUpdateEmailScreen(addToBackStack: Boolean = true)
    fun goToUpdatePhoneScreen(addToBackStack: Boolean = true)
}
