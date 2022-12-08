package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class ReferralAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    data class ReferralProgramClicked(val launchOrigin: Origin) : ReferralAnalyticsEvents(
        event = AnalyticsNames.REFERRAL_PROGRAM_CLICKED.eventName,
        params = mapOf(
            KEY_PLATFORM to WALLET,
            KEY_ORIGIN to launchOrigin.name
        )
    )

    data class ReferralCodeFilled(val code: String) : ReferralAnalyticsEvents(
        event = AnalyticsNames.REFERRAL_VIEW_REFERRAL.eventName,
        params = mapOf(
            KEY_PLATFORM to WALLET,
            KEY_CODE to code
        )
    )

    companion object {
        private const val WALLET = "wallet"
        private const val KEY_PLATFORM = "platform"
        private const val KEY_CODE = "code"
        private const val KEY_ORIGIN = "origin"
    }
}

enum class Origin {
    Profile, Portfolio, PopupSheet, Deeplink
}
