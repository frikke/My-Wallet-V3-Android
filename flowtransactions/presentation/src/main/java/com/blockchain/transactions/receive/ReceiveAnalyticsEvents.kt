package com.blockchain.transactions.receive

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.presentation.analytics.TxFlowAnalyticsAccountType
import java.io.Serializable

sealed class ReceiveAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    class ReceiveAccountSelected(
        private val accountType: TxFlowAnalyticsAccountType,
        private val networkTicker: String
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.RECEIVE_ACCOUNT_SELECTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "account_type" to accountType.name,
                "currency" to networkTicker
            )
    }

    class ReceiveDetailsCopied(
        private val accountType: TxFlowAnalyticsAccountType,
        private val networkTicker: String
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.RECEIVE_ADDRESS_COPIED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "account_type" to accountType.name,
                "currency" to networkTicker
            )
    }
}
