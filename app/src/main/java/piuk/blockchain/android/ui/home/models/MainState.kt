package piuk.blockchain.android.ui.home.models

import androidx.annotation.StringRes
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.domain.paymentmethods.model.BankLinkingInfo
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint

data class MainState(
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val deeplinkResult: DeepLinkResult = DeepLinkResult.DeepLinkResultUnknownLink(),
    val walletMode: WalletMode? = null,
    val referral: ReferralState = ReferralState(ReferralInfo.NotAvailable),
    val isEarnOnNavEnabled: Boolean = false
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    object LaunchTwoFaSetup : ViewToLaunch()
    object LaunchVerifyEmail : ViewToLaunch()
    object LaunchSetupBiometricLogin : ViewToLaunch()
    class LaunchInterestDashboard(val origin: LaunchOrigin) : ViewToLaunch()
    class LaunchSimpleBuy(val asset: AssetInfo) : ViewToLaunch()
    class LaunchKyc(val entryPoint: KycEntryPoint) : ViewToLaunch()
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

    class LaunchWalletConnectSessionNetworkSelection(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    class LaunchWalletConnectSessionApproval(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    class LaunchWalletConnectSessionApprovalWithNetwork(
        val walletConnectSession: WalletConnectSession,
        val networkInfo: NetworkInfo
    ) : ViewToLaunch()

    class LaunchWalletConnectSessionApproved(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    class LaunchWalletConnectSessionRejected(val walletConnectSession: WalletConnectSession) : ViewToLaunch()
    object LaunchSimpleBuyFromDeepLinkApproval : ViewToLaunch()
    class LaunchPaymentForCancelledOrder(val state: SimpleBuyState) : ViewToLaunch()
    class CheckForAccountWalletLinkErrors(val walletIdHint: String) : ViewToLaunch()
    class LaunchTransactionFlowWithTargets(val targets: Collection<CryptoTarget>) : ViewToLaunch()
    class ShowTargetScanError(val error: QrScanError) : ViewToLaunch()
    object ShowReferralSheet : ViewToLaunch()
    class LaunchTxFlowWithAccountForAction(val account: LaunchFlowForAccount, val action: AssetAction) : ViewToLaunch()
    class LaunchRewardsSummaryFromDeepLink(val account: LaunchFlowForAccount) : ViewToLaunch()
}

sealed class LaunchFlowForAccount {
    class SourceAccount(val source: BlockchainAccount) : LaunchFlowForAccount()
    class TargetAccount(val target: TransactionTarget) : LaunchFlowForAccount()
    class SourceAndTargetAccount(val sourceAccount: BlockchainAccount, val targetAccount: TransactionTarget) :
        LaunchFlowForAccount()

    object NoAccount : LaunchFlowForAccount()
}
