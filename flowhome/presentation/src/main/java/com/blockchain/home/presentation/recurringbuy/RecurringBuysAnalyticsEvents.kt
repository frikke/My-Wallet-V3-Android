package com.blockchain.home.presentation.recurringbuy

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class RecurringBuysAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object CoinviewCtaClicked : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_COINVIEW_CTA_CLICKED.eventName
    )

    object HomeCtaClicked : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_HOME_CTA_CLICKED.eventName
    )

    object OnboardingViewed : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_ONBOARDING_VIEWED.eventName
    )

    data class HomeDetailClicked(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_HOME_DETAIL_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class DashboardDetailClicked(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_DASHBOARD_DETAIL_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    object ManageClicked : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_HOME_MANAGE_CLICKED.eventName
    )

    object DashboardAddClicked : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_DASHBOARD_ADD_CLICKED.eventName
    )

    data class DetailViewed(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_DETAIL_VIEWED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class CancelClicked(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_DETAIL_CANCEL_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class BuyToggleClicked(
        val ticker: String,
        val toggle: Boolean
    ) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_BUY_TOGGLE_CLICKED.eventName,
        params = mapOf(
            CURRENCY to ticker,
            TOGGLE to toggle.toString()
        )
    )

    data class BuyFrequencyClicked(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_BUY_TOGGLE_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class BuyFrequencyViewed(val ticker: String) : RecurringBuysAnalyticsEvents(
        event = AnalyticsNames.RB_BUY_TOGGLE_CLICKED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    companion object {
        private const val CURRENCY = "currency"
        private const val TOGGLE = "toggle"
    }
}
