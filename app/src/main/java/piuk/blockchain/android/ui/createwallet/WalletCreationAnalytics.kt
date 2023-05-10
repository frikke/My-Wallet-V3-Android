package piuk.blockchain.android.ui.createwallet

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.extensions.filterNotNullValues

sealed class WalletCreationAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    class WalletSignUp(
        countryName: String,
        stateIso: String?
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP.eventName,
        params = mapOf(
            COUNTRY to countryName,
            STATE to stateIso
        ).filterNotNullValues()
    )

    class CountrySelectedOnSignUp(
        countryIso: String
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP_COUNTRY_SELECTED.eventName,
        params = mapOf(COUNTRY to countryIso)
    )

    class StateSelectedOnSignUp(
        stateIso: String
    ) : WalletCreationAnalytics(
        event = AnalyticsNames.WALLET_SIGN_UP_STATE_SELECTED.eventName,
        params = mapOf(STATE to stateIso)
    )

    companion object {
        private const val COUNTRY = "country"
        private const val STATE = "country_state"
    }
}
