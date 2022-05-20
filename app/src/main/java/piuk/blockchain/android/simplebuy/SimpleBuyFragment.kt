package piuk.blockchain.android.simplebuy

import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.base.FlowFragment
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import info.blockchain.balance.AssetInfo

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator :
    SlidingModalBottomDialog.Host,
    SmallSimpleBuyNavigator,
    ErrorBuyNavigator {
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
    fun goToSetupFirstRecurringBuy(addToBackStack: Boolean = true)
    fun goToFirstRecurringBuyCreated(addToBackStack: Boolean = true)
    fun goToBlockedBuyScreen()
}

interface SmallSimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun popFragmentsInStackUntilFind(fragmentName: String, popInclusive: Boolean)
}

interface ErrorBuyNavigator {
    fun showErrorInBottomSheet(
        title: String,
        description: String,
        button: String? = null,
        error: String,
        errorDescription: String? = null,
        nabuApiException: NabuApiException? = null
    )
}
