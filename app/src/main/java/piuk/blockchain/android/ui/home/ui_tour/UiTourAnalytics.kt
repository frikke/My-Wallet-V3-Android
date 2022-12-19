package piuk.blockchain.android.ui.home.ui_tour

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class UiTourAnalytics : AnalyticsEvent {
    object Viewed : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_VIEWED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }
    class CtaClicked(val step: UiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_CTA_CLICKED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }
    class ProgressClicked(val step: UiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_PROGRESS_CLICKED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }
    class Dismissed(val step: UiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_DISMISSED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }

    class NewTourCtaClicked(val step: NewUiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_CTA_CLICKED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }
    class NewTourProgressClicked(val step: NewUiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_PROGRESS_CLICKED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }
    class NewTourDismissed(val step: NewUiTourStep) : UiTourAnalytics() {
        override val event: String = AnalyticsNames.UI_TOUR_DISMISSED.eventName
        override val params: Map<String, Serializable> = mapOf("step" to step.toAnalyticsId())
    }

    protected fun UiTourStep.toAnalyticsId() = when (this) {
        UiTourStep.BUY_AND_SELL -> 4
        UiTourStep.PRICES -> 2
        UiTourStep.MIDDLE_BUTTON -> 3
        UiTourStep.ACTIVITY -> 5
        UiTourStep.BUYER_HANDHOLD -> 1
    }.toString()

    protected fun NewUiTourStep.toAnalyticsId() = when (this) {
        NewUiTourStep.EARN -> 4
        NewUiTourStep.PRICES -> 2
        NewUiTourStep.MIDDLE_BUTTON -> 3
        NewUiTourStep.ACTIVITY -> 5
        NewUiTourStep.BUYER_HANDHOLD -> 1
    }.toString()
}
