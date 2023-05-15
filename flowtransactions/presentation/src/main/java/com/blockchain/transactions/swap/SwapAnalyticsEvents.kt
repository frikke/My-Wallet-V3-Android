package com.blockchain.transactions.swap

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CustodialTradingAccount

sealed class SwapAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
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
        event = AnalyticsNames.SWAP_ENTER_AMOUNT_TARGET_CLICKED.eventName
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
        event = AnalyticsNames.SWAP_TARGET_SELECTED.eventName,
        params = mapOf(CURRENCY to ticker)
    )

    object ConfirmationViewed : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_CONFIRMATION_VIEWED.eventName
    )

    data class SwapClicked(
        val fromTicker: String,
        val fromAmount: String,
        val toTicker: String,
        val destination: AccountType
    ) : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_CONFIRMATION_SWAP_CLICKED.eventName,
        params = mapOf(
            INPUT_CURRENCY to fromTicker,
            INPUT_AMOUNT to fromAmount,
            OUTPUT_CURRENCY to toTicker,
            DESTINATION to destination.name
        )
    )

    object PendingViewed : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_CONFIRMATION_PENDING_VIEWED.eventName
    )

    object SuccessViewed : SwapAnalyticsEvents(
        event = AnalyticsNames.SWAP_CONFIRMATION_SUCCESS_VIEWED.eventName
    )

    enum class AccountType {
        USERKEY, TRADING
    }

    companion object {
        private const val CURRENCY = "currency"
        private const val INPUT_CURRENCY = "input_currency"
        private const val INPUT_AMOUNT = "input_amount"
        private const val OUTPUT_CURRENCY = "output_currency"
        private const val DESTINATION = "destination"

        fun CryptoAccount.accountType(): AccountType {
            return if (this is CustodialTradingAccount) AccountType.TRADING else AccountType.USERKEY
        }
    }
}
