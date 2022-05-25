package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStepState

data class DashboardOnboardingState(
    val steps: List<CompletableDashboardOnboardingStep> =
        DashboardOnboardingStep.values().map {
            CompletableDashboardOnboardingStep(it, DashboardOnboardingStepState.INCOMPLETE)
        },
    val errorState: DashboardOnboardingError = DashboardOnboardingError.None,
    val navigationAction: DashboardOnboardingNavigationAction = DashboardOnboardingNavigationAction.None
) : MviState

sealed class DashboardOnboardingNavigationAction {
    object None : DashboardOnboardingNavigationAction()
    object StartKyc : DashboardOnboardingNavigationAction()
    data class AddPaymentMethod(val eligiblePaymentMethods: List<PaymentMethod>) : DashboardOnboardingNavigationAction()
    object OpenBuy : DashboardOnboardingNavigationAction()
    data class SelectTradingCurrency(val supportedCurrencies: List<FiatCurrency>, val selectedCurrency: FiatCurrency) :
        DashboardOnboardingNavigationAction()

    object AddCard : DashboardOnboardingNavigationAction()
    data class WireTransferAccountDetails(val currency: FiatCurrency) : DashboardOnboardingNavigationAction()
    data class LinkBank(val linkBankTransfer: LinkBankTransfer) : DashboardOnboardingNavigationAction()
}

sealed class DashboardOnboardingError {
    object None : DashboardOnboardingError()
    data class Error(val throwable: Throwable) : DashboardOnboardingError()
}
