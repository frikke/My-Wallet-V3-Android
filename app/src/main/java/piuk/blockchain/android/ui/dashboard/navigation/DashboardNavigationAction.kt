package piuk.blockchain.android.ui.dashboard.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.nabu.BlockedReason
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.dashboard.model.DashboardIntent
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails

sealed class DashboardNavigationAction {
    object AppRating : DashboardNavigationAction()
    data class BackUpBeforeSend(val backupSheetDetails: BackupDetails) : DashboardNavigationAction(), BottomSheet
    object SimpleBuyCancelOrder : DashboardNavigationAction(), BottomSheet
    data class FiatFundsDetails(val fiatAccount: FiatAccount) : DashboardNavigationAction(), BottomSheet
    data class LinkOrDeposit(val fiatAccount: FiatAccount? = null) : DashboardNavigationAction(), BottomSheet
    data class LinkWithAlias(val fiatAccount: FiatAccount? = null) : DashboardNavigationAction()
    object FiatFundsNoKyc : DashboardNavigationAction(), BottomSheet
    data class FiatDepositOrWithdrawalBlockedDueToSanctions(
        val reason: BlockedReason.Sanctions
    ) : DashboardNavigationAction(), BottomSheet
    data class DepositQuestionnaire(
        val questionnaire: Questionnaire,
        val callbackIntent: DashboardIntent.LaunchBankTransferFlow
    ) : DashboardNavigationAction(), BottomSheet
    data class InterestSummary(
        val account: CryptoAccount
    ) : DashboardNavigationAction(), BottomSheet

    data class PaymentMethods(
        val paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) : DashboardNavigationAction(), BottomSheet

    class LinkBankWithPartner(
        val linkBankTransfer: LinkBankTransfer,
        val fiatAccount: FiatAccount,
        val assetAction: AssetAction
    ) : DashboardNavigationAction()

    data class DashboardOnboarding(
        val initialSteps: List<CompletableDashboardOnboardingStep>
    ) : DashboardNavigationAction()

    class TransactionFlow(
        val sourceAccount: BlockchainAccount = NullCryptoAccount(),
        val target: TransactionTarget = NullCryptoAccount(),
        val action: AssetAction
    ) : DashboardNavigationAction()

    class Coinview(val asset: AssetInfo) : DashboardNavigationAction()

    interface BottomSheet
}
