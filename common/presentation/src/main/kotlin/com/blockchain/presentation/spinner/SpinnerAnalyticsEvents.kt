package com.blockchain.presentation.spinner

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

internal sealed class SpinnerAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    data class SpinnerState(
        val flowId: String,
        val duration: Int,
        val screen: SpinnerAnalyticsScreen,
        val screenEvent: SpinnerAnalyticsAction,
        val state: SpinnerAnalyticsState,
    ) : SpinnerAnalyticsEvents(
        event = AnalyticsNames.SPINNER_LAUNCHED.eventName,
        params = mapOf(
            FLOW_ID to flowId,
            DURATION to duration.toString(),
            SCREEN to screen.name,
            SCREEN_EVENT to screenEvent.name,
            SPINNER_STATE to state.name,
        )
    )

    companion object {
        private const val FLOW_ID = "flow_id"
        private const val DURATION = "duration"
        private const val SCREEN = "screen"
        private const val SCREEN_EVENT = "screen_event"
        private const val SPINNER_STATE = "spinner_state"
    }
}
