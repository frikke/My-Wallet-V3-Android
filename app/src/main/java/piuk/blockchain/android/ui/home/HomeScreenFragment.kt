package piuk.blockchain.android.ui.home

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.sell.BuySellFragment

interface HomeScreenFragment {
    fun navigator(): HomeNavigator
}

interface HomeNavigator {
    fun launchSwap(
        sourceAccount: CryptoAccount? = null,
        targetAccount: CryptoAccount? = null
    )

    fun launchKyc(campaignType: CampaignType)
    fun launchBackupFunds(fragment: Fragment? = null, requestCode: Int = 0)
    fun launchSetup2Fa()
    fun launchVerifyEmail()
    fun launchSetupFingerprintLogin()
    fun launchReceive()
    fun launchSend()
    fun launchBuySell(
        viewType: BuySellFragment.BuySellViewType = BuySellFragment.BuySellViewType.TYPE_BUY,
        asset: AssetInfo? = null,
        reload: Boolean = false
    )
    fun launchSimpleBuy(asset: AssetInfo, paymentMethodId: String? = null)
    fun launchInterestDashboard(origin: LaunchOrigin)
    fun launchFiatDeposit(currency: String)
    fun launchTransfer()
    fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo)
    fun launchSimpleBuyFromDeepLinkApproval()
    fun launchPendingVerificationScreen(campaignType: CampaignType)

    fun performAssetActionFor(action: AssetAction, account: BlockchainAccount? = null)
    fun resumeSimpleBuyKyc()
}

abstract class HomeScreenMviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    MviFragment<M, I, S, E>(),
    HomeScreenFragment {

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}
