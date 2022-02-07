package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep

sealed class DashboardOnboardingAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = mapOf()
) : AnalyticsEvent {
    data class Viewed(
        private val currentStepIndex: Int
    ) : DashboardOnboardingAnalytics(
        event = AnalyticsNames.DASHBOARD_ONBOARDING_VIEWED.eventName,
        params = mapOf("current_step_completed" to currentStepIndex.toString())
    )
    data class Dismissed(
        private val currentStepIndex: Int
    ) : DashboardOnboardingAnalytics(
        event = AnalyticsNames.DASHBOARD_ONBOARDING_DISMISSED.eventName,
        params = mapOf("current_step_completed" to currentStepIndex.toString())
    )
    data class CardClicked(
        private val currentStepIndex: Int
    ) : DashboardOnboardingAnalytics(
        event = AnalyticsNames.DASHBOARD_ONBOARDING_CARD_CLICKED.eventName,
        params = mapOf("current_step_completed" to currentStepIndex.toString())
    )
    data class StepLaunched(
        private val currentStepIndex: Int,
        private val step: DashboardOnboardingStep,
        // Did the user click on the bottom next step button or the step itself
        private val nextStepButtonClicked: Boolean
    ) : DashboardOnboardingAnalytics(
        event = AnalyticsNames.DASHBOARD_ONBOARDING_STEP_LAUNCHED.eventName,
        params = mapOf(
            "current_step_completed" to currentStepIndex.toString(),
            "item" to step.toParam(),
            "button_clicked" to nextStepButtonClicked
        )
    )
}

fun List<CompletableDashboardOnboardingStep>.toCurrentStepIndex(): Int? =
    indexOfLast { it.isCompleted }.takeIf { it != -1 }

private fun DashboardOnboardingStep.toParam(): String = when (this) {
    DashboardOnboardingStep.UPGRADE_TO_GOLD -> "VERIFY"
    DashboardOnboardingStep.LINK_PAYMENT_METHOD -> "LINK_PAYMENT"
    DashboardOnboardingStep.BUY -> "BUY_CRYPTO"
}
