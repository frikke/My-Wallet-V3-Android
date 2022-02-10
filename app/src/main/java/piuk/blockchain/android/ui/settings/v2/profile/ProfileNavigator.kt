package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.commonarch.presentation.base.FlowFragment

interface ProfileNavigatorScreen : FlowFragment {
    fun navigator(): ProfileNavigator
}

interface ProfileNavigator {
    fun goToUpdateEmailScreen(addToBackStack: Boolean = true)
    fun goToUpdatePhoneScreen(addToBackStack: Boolean = true)
}
