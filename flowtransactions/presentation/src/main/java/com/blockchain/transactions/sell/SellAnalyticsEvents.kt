package com.blockchain.transactions.sell

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import info.blockchain.balance.CurrencyType

sealed class SellAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object SelectSourceViewed : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_SELECT_SOURCE_VIEWED.eventName
    )

    data class EnterAmountViewed(
        private val fromTicker: String
    ) : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_ENTER_AMOUNT_VIEWED.eventName,
        params = mapOf(
            CURRENCY to fromTicker
        )
    )

    data class EnterAmountQuickFillClicked(
        private val isPartial: Boolean
    ) : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_ENTER_AMOUNT_QUICK_FILL_CLICKED.eventName,
        params = mapOf(
            FILL to if (isPartial) "partial" else "max"
        )
    )

    data class EnterAmountPreviewClicked(
        private val inputType: CurrencyType
    ) : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_ENTER_AMOUNT_PREVIEW_CLICKED.eventName,
        params = mapOf(
            INPUT_TYPE to inputType.name
        )
    )

    data class ConfirmationViewed(
        private val fromTicker: String
    ) : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_CONFIRMATION_VIEWED.eventName,
        params = mapOf(
            CURRENCY to fromTicker
        )
    )

    data class ConfirmationSellClicked(
        val fromTicker: String,
        val fromAmount: String,
        val toTicker: String
    ) : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_CONFIRMATION_SELL_CLICKED.eventName,
        params = mapOf(
            INPUT_CURRENCY to fromTicker,
            INPUT_AMOUNT to fromAmount,
            OUTPUT_CURRENCY to toTicker
        )
    )

    object PendingViewed : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_CONFIRMATION_PENDING_VIEWED.eventName
    )

    object ErrorViewed : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_CONFIRMATION_ERROR_VIEWED.eventName
    )

    object SuccessViewed : SellAnalyticsEvents(
        event = AnalyticsNames.SELL_CONFIRMATION_SUCCESS_VIEWED.eventName
    )

    companion object {
        private const val CURRENCY = "currency"
        private const val INPUT_CURRENCY = "input_currency"
        private const val INPUT_TYPE = "input_type"
        private const val INPUT_AMOUNT = "input_amount"
        private const val OUTPUT_CURRENCY = "output_currency"
        private const val FILL = "Fill"
    }
}
