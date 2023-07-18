package com.blockchain.presentation.spinner

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

internal sealed class SpinnerAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    data class SpinnerTimeout(
        val duration: Int,
        val screen: SpinnerAnalyticsScreen,
    ) : SpinnerAnalyticsEvents(
        event = AnalyticsNames.SPINNER_TIMEOUT.eventName,
        params = mapOf(
            DURATION to duration.toString(),
            SCREEN to screen.name,
        )
    )

    companion object {
        private const val DURATION = "duration"
        private const val SCREEN = "screen"
    }
}
