package piuk.blockchain.android.ui.settings.profile

interface ProfileNavigatorScreen {
    fun navigator(): ProfileNavigator
}

interface ProfileNavigator {
    fun goToUpdateEmailScreen(addToBackStack: Boolean = true)
    fun goToUpdatePhoneScreen(addToBackStack: Boolean = true)
}
