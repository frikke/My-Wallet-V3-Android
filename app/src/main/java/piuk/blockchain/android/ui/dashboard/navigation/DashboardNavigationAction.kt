package piuk.blockchain.android.ui.dashboard.navigation

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.nabu.models.data.LinkBankTransfer
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.model.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails

sealed class DashboardNavigationAction {
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

    interface BottomSheet
}
