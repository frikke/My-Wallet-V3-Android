package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.PaymentMethodType

sealed class DashboardOnboardingIntent : MviIntent<DashboardOnboardingState> {

    object FetchSteps : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState
    }

    data class FetchStepsSuccess(
        private val steps: List<CompletableDashboardOnboardingStep>
    ) : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState.copy(
            steps = steps
        )
    }

    data class StepClicked(val clickedStep: DashboardOnboardingStep) : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState
    }

    data class FetchFailed(private val error: Throwable) : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState.copy(
            error = error
        )
    }

    data class PaymentMethodClicked(val type: PaymentMethodType) : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState
    }

    data class NavigateTo(private val navigationAction: DashboardOnboardingNavigationAction) :
        DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState = oldState.copy(
            navigationAction = navigationAction
        )
    }

    object ClearNavigation : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState =
            oldState.copy(navigationAction = null)
    }

    object ClearError : DashboardOnboardingIntent() {
        override fun reduce(oldState: DashboardOnboardingState): DashboardOnboardingState =
            oldState.copy(error = null)
    }
}
