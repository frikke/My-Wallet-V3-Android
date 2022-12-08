package com.blockchain.domain.onboarding

data class CompletableDashboardOnboardingStep(
    val step: DashboardOnboardingStep,
    val state: DashboardOnboardingStepState
) {
    val isCompleted: Boolean = state == DashboardOnboardingStepState.COMPLETE
}

enum class DashboardOnboardingStepState {
    INCOMPLETE,
    PENDING,
    COMPLETE
}

enum class DashboardOnboardingStep {
    UPGRADE_TO_GOLD,
    LINK_PAYMENT_METHOD,
    BUY
}
