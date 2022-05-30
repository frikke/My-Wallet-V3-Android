package piuk.blockchain.android.ui.home.models

import android.content.Intent
import androidx.annotation.StringRes
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoTarget
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.walletconnect.domain.WalletConnectSession
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager

data class MainState(
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val deeplinkResult: DeepLinkResult = DeepLinkResult.DeepLinkResultFailed,
    val deeplinkIntent: Intent? = null
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    object LaunchSwap : ViewToLaunch()
    object LaunchTwoFaSetup : ViewToLaunch()
    object LaunchVerifyEmail : ViewToLaunch()
    object LaunchSetupBiometricLogin : ViewToLaunch()
    class LaunchInterestDashboard(val origin: LaunchOrigin) : ViewToLaunch()
    object LaunchReceive : ViewToLaunch()
    object LaunchSend : ViewToLaunch()
    class LaunchBuySell(val type: BuySellFragment.BuySellViewType, val asset: AssetInfo?) : ViewToLaunch()
    class LaunchAssetAction(val action: AssetAction, val account: BlockchainAccount?) : ViewToLaunch()
    class LaunchSimpleBuy(val asset: AssetInfo) : ViewToLaunch()
    class LaunchKyc(val campaignType: CampaignType) : ViewToLaunch()
    class DisplayAlertDialog(@StringRes val dialogTitle: Int, @StringRes val dialogMessage: Int) : ViewToLaunch()
    object ShowOpenBankingError : ViewToLaunch()
    class LaunchOpenBankingLinking(val bankLinkingInfo: BankLinkingInfo) : ViewToLaunch()
    object LaunchOpenBankingBuyApprovalError : ViewToLaunch()
    class LaunchOpenBankingApprovalDepositInProgress(val value: Money) : ViewToLaunch()
    class LaunchOpenBankingApprovalTimeout(val currencyCode: String) : ViewToLaunch()
    class LaunchOpenBankingError(val currencyCode: String) : ViewToLaunch()
    class LaunchServerDrivenOpenBankingError(val currencyCode: String, val title: String, val description: String) :
        ViewToLaunch()

    class LaunchOpenBankingApprovalDepositComplete(val amount: Money, val estimatedDepositCompletionTime: String) :
        ViewToLaunch()

    class LaunchWalletConnectSessionApproval(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    class LaunchWalletConnectSessionApproved(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    class LaunchWalletConnectSessionRejected(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    object LaunchSimpleBuyFromDeepLinkApproval : ViewToLaunch()
    class LaunchPaymentForCancelledOrder(val state: SimpleBuyState) : ViewToLaunch()
    class CheckForAccountWalletLinkErrors(val walletIdHint: String) : ViewToLaunch()
    class LaunchUpsellAssetAction(val upsell: KycUpgradePromptManager.Type) : ViewToLaunch()
    class LaunchTransactionFlowWithTargets(val targets: Collection<CryptoTarget>) : ViewToLaunch()
    class ShowTargetScanError(val error: QrScanError) : ViewToLaunch()
    object ShowUiTour : ViewToLaunch()
    object ShowEntitySwitchSilverKycUpsell : ViewToLaunch()
}
