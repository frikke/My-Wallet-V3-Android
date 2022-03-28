package piuk.blockchain.android.simplebuy

import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import info.blockchain.balance.AssetInfo

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator : SlidingModalBottomDialog.Host, SmallSimpleBuyNavigator {
    fun goToBuyCryptoScreen(
        addToBackStack: Boolean = true,
        preselectedAsset: AssetInfo,
        preselectedPaymentMethodId: String?,
        preselectedAmount: String?
    )
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun startKyc()
    fun pop()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToPendingOrderScreen()
    fun goToPaymentScreen(addToBackStack: Boolean = true, isPaymentAuthorised: Boolean = false)
    fun launchBankAuthWithError(errorState: ErrorState)
    fun goToSetupFirstRecurringBuy(addToBackStack: Boolean = true)
    fun goToFirstRecurringBuyCreated(addToBackStack: Boolean = true)
}

interface SmallSimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun popFragmentsInStackUntilFind(fragmentName: String, popInclusive: Boolean)
}
