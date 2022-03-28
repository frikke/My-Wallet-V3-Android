package piuk.blockchain.android.ui.home.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

object EntitySwitchSilverKycUpsellViewed : AnalyticsEvent {
    override val event: String = AnalyticsNames.ENTITY_SWITCH_SILVER_KYC_UPSELL_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object EntitySwitchSilverKycUpsellCtaClicked : AnalyticsEvent {
    override val event: String = AnalyticsNames.ENTITY_SWITCH_SILVER_KYC_UPSELL_CTA_CLICKED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object EntitySwitchSilverKycUpsellDismissed : AnalyticsEvent {
    override val event: String = AnalyticsNames.ENTITY_SWITCH_SILVER_KYC_UPSELL_DISMISSED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}
