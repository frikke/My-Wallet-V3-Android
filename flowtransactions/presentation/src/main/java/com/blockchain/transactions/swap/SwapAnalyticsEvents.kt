package com.blockchain.transactions.swap

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.walletmode.WalletMode

sealed class SwapAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object EnterAmountViewed : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_VIEWED.eventName
    )

    object MaxClicked : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_MAX_CLICKED.eventName
    )

    object PreviewClicked : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_PREVIEW_CLICKED.eventName
    )

    object SelectSourceClicked : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_SOURCE_CLICKED.eventName
    )

    object SelectDestinationClicked : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_DESTINATION_CLICKED.eventName
    )

    data class SourceAccountSelected(
        val ticker: String
    ) : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_SOURCE_SELECTED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    data class DestinationAccountSelected(
        val ticker: String
    ) : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_DESTINATION_SELECTED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    object ConfirmationViewed : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_CONFIRMATION_VIEWED.eventName
    )

    companion object{
        private const val CURRENCY = "currency"
    }
}
