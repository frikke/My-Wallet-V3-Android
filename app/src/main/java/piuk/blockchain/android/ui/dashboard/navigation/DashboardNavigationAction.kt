package piuk.blockchain.android.ui.dashboard.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.core.payments.model.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.android.ui.home.models.ViewToLaunch

sealed class DashboardNavigationAction {
    object AppRating : DashboardNavigationAction()
    object StxAirdropComplete : DashboardNavigationAction(), BottomSheet
    data class BackUpBeforeSend(val backupSheetDetails: BackupDetails) : DashboardNavigationAction(), BottomSheet
    object SimpleBuyCancelOrder : DashboardNavigationAction(), BottomSheet
    data class FiatFundsDetails(val fiatAccount: FiatAccount) : DashboardNavigationAction(), BottomSheet
    data class LinkOrDeposit(val fiatAccount: FiatAccount? = null) : DashboardNavigationAction(), BottomSheet
    object FiatFundsNoKyc : DashboardNavigationAction(), BottomSheet
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
    ) : DashboardNavigationAction(), FullScreenFlow

    class Coinview(val asset: AssetInfo) : DashboardNavigationAction(), FullScreenFlow

    interface BottomSheet
    interface FullScreenFlow
}
