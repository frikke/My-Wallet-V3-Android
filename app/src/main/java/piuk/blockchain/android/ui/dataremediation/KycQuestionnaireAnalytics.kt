package piuk.blockchain.android.ui.dataremediation

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

object KycQuestionnaireViewed : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_QUESTIONNAIRE_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object KycQuestionnaireSubmitted : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_QUESTIONNAIRE_SUBMITTED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}
