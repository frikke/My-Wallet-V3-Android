package com.blockchain.presentation.onboarding

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class OnboardingAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object OnboardingViewed : OnboardingAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_ONBOARDING_VIEWED.eventName
    )

    object OnboardingContinueClicked : OnboardingAnalyticsEvents(
        event = AnalyticsNames.SUPERAPP_DEFI_ONBOARDING_CONTINUE_CLICKED.eventName
    )
}
