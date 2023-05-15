package piuk.blockchain.android.simplebuy

import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import info.blockchain.balance.AssetInfo

interface SimpleBuyScreen : SlidingModalBottomDialog.Host {
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
        preselectedAmount: String?,
        launchLinkCard: Boolean = false,
        launchPaymentMethodSelection: Boolean = false,
        preselectedFiatTicker: String? = null
    )

    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun startKyc()
    fun pop()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToPendingOrderScreen()
    fun goToPaymentScreen(
        addToBackStack: Boolean = true,
        isPaymentAuthorised: Boolean = false,
        showRecurringBuySuggestion: Boolean = false,
        recurringBuyFrequencyRemote: RecurringBuyFrequency? = RecurringBuyFrequency.ONE_TIME
    )
    fun goToBlockedBuyScreen()
}

interface SmallSimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun popFragmentsInStackUntilFind(fragmentName: String, popInclusive: Boolean)
    fun launchUpSellBottomSheet(assetBoughtTicker: String)
}

interface ErrorBuyNavigator {
    fun showErrorInBottomSheet(
        title: String,
        description: String,
        error: String,
        errorDescription: String? = null,
        nabuApiException: NabuApiException? = null,
        serverSideUxErrorInfo: ServerSideUxErrorInfo? = null,
        closeFlowOnDeeplinkFallback: Boolean = true
    )

    fun showBankRefreshError(accountId: String)
}
