package piuk.blockchain.android.ui.home

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.common.model.BuySellViewType
import com.blockchain.domain.paymentmethods.model.BankLinkingInfo
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.campaign.CampaignType

interface HomeScreenFragment {
    fun navigator(): HomeNavigator
}

interface HomeNavigator {
    fun launchKyc(campaignType: CampaignType)
    fun launchBackupFunds(fragment: Fragment? = null, requestCode: Int = 0)
    fun launchSetup2Fa()
    fun launchOpenExternalEmailApp()
    fun launchSetupFingerprintLogin()
    fun launchBuySell(
        viewType: BuySellViewType = BuySellViewType.TYPE_BUY,
        asset: AssetInfo? = null,
        reload: Boolean = false
    )
    fun launchSimpleBuy(asset: AssetInfo, paymentMethodId: String? = null)
    fun launchFiatDeposit(currency: String)
    fun launchTransfer()
    fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo)
    fun launchSimpleBuyFromDeepLinkApproval()
    fun launchPendingVerificationScreen(campaignType: CampaignType)

    fun resumeSimpleBuyKyc()
}

abstract class HomeScreenMviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    MviFragment<M, I, S, E>(),
    HomeScreenFragment {

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}
