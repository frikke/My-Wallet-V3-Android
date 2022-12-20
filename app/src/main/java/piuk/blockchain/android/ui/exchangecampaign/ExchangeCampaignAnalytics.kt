package piuk.blockchain.android.ui.exchangecampaign

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class ExchangeCampaignAnalytics(
    override val event: String,
    override val params: Map<String, Serializable>
) : AnalyticsEvent {

    data class ExchangeAwarenessPromptShown(
        val countOfPrompts: Int,
        val isSSO: Boolean,
    ) : ExchangeCampaignAnalytics(
        event = AnalyticsNames.EXCHANGE_AWARENESS_PROMPT_SHOWN.eventName,
        params = mapOf(
            COUNT_OF_PROMPTS_KEY to countOfPrompts,
            CURRENT_ORIGIN_KEY to ORIGIN_WALLET_VALUE,
            SSO_USER_KEY to isSSO
        )
    )

    data class ExchangeAwarenessPromptClicked(
        val isSSO: Boolean
    ) : ExchangeCampaignAnalytics(
        event = AnalyticsNames.EXCHANGE_AWARENESS_PROMPT_CLICKED.eventName,
        params = mapOf(
            CURRENT_ORIGIN_KEY to ORIGIN_WALLET_VALUE,
            SSO_USER_KEY to isSSO
        )
    )

    data class ExchangeAwarenessPromptDismissed(
        val isSSO: Boolean
    ) : ExchangeCampaignAnalytics(
        event = AnalyticsNames.EXCHANGE_AWARENESS_PROMPT_DISMISSED.eventName,
        params = mapOf(
            CURRENT_ORIGIN_KEY to ORIGIN_WALLET_VALUE,
            SSO_USER_KEY to isSSO
        )
    )

    companion object {

        private const val COUNT_OF_PROMPTS_KEY = "count_of_prompts"
        private const val CURRENT_ORIGIN_KEY = "current_origin"
        private const val SSO_USER_KEY = "sso_user"

        private const val ORIGIN_WALLET_VALUE = "Wallet-Prompt"
    }
}
