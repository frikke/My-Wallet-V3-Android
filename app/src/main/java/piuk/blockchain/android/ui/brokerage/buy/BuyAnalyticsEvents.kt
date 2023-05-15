package piuk.blockchain.android.ui.brokerage.buy

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames

sealed class BuyAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    data class TopMoverAssetClicked(
        val ticker: String,
        val percentageMove: Double,
        val position: Int
    ) : BuyAnalyticsEvents(
        event = AnalyticsNames.TOP_MOVER_BUY_CLICKED.eventName,
        params = mapOf(
            CURRENCY to ticker,
            PERCENTAGE_MOVE to percentageMove.toString(),
            POSITION to position.toString()
        )
    )

    companion object {
        private const val CURRENCY = "currency"
        private const val PERCENTAGE_MOVE = "percentage move"
        private const val POSITION = "position"
    }
}
