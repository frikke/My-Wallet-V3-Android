package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.models.data.LinkBankTransfer
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.ui.base.mvi.MviState

data class DashboardOnboardingState(
    val steps: List<CompletableDashboardOnboardingStep> =
        DashboardOnboardingStep.values().map { CompletableDashboardOnboardingStep(it, false) },
    val errorState: DashboardOnboardingError = DashboardOnboardingError.None,
    val navigationAction: DashboardOnboardingNavigationAction = DashboardOnboardingNavigationAction.None
) : MviState

sealed class DashboardOnboardingNavigationAction {
    object None : DashboardOnboardingNavigationAction()
    object StartKyc : DashboardOnboardingNavigationAction()
    data class AddPaymentMethod(val eligiblePaymentMethods: List<PaymentMethod>) : DashboardOnboardingNavigationAction()
    object OpenBuy : DashboardOnboardingNavigationAction()
    data class SelectTradingCurrency(val supportedCurrencies: List<String>, val selectedCurrency: String) :
        DashboardOnboardingNavigationAction()

    object AddCard : DashboardOnboardingNavigationAction()
    data class WireTransferAccountDetails(val currency: String) : DashboardOnboardingNavigationAction()
    data class LinkBank(val linkBankTransfer: LinkBankTransfer) : DashboardOnboardingNavigationAction()
}

sealed class DashboardOnboardingError {
    object None : DashboardOnboardingError()
    data class Error(val throwable: Throwable) : DashboardOnboardingError()
}
