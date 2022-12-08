package com.blockchain.domain.onboarding

interface OnBoardingStepsService {
    suspend fun onBoardingSteps(): List<CompletableDashboardOnboardingStep>
}
