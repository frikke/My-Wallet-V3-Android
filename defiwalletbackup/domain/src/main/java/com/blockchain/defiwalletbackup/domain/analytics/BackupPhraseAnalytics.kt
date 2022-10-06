package com.blockchain.defiwalletbackup.domain.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

object BackupPhraseAnalytics {

    val enableDefiClicked = object : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.ENABLE_DEFI_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = emptyMap()
    }
}
