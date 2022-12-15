package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import info.blockchain.balance.FiatCurrency

data class DashboardOnboardingState(
    val steps: List<CompletableDashboardOnboardingStep> =
        DashboardOnboardingStep.values().map {
            CompletableDashboardOnboardingStep(it, DashboardOnboardingStepState.INCOMPLETE)
        },
    val error: Throwable? = null,
    val navigationAction: DashboardOnboardingNavigationAction? = null
) : MviState

sealed class DashboardOnboardingNavigationAction {
    object StartKyc : DashboardOnboardingNavigationAction()
    data class AddPaymentMethod(val eligiblePaymentMethods: List<PaymentMethod>) : DashboardOnboardingNavigationAction()
    object OpenBuy : DashboardOnboardingNavigationAction()

    object AddCard : DashboardOnboardingNavigationAction()
    data class WireTransferAccountDetails(val currency: FiatCurrency) : DashboardOnboardingNavigationAction()
    data class LinkBank(val linkBankTransfer: LinkBankTransfer) : DashboardOnboardingNavigationAction()
}
