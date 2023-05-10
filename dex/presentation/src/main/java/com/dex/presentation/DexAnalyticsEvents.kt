package com.dex.presentation

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class DexAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap(),
) : AnalyticsEvent {

    object OnboardingViewed : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_ONBOARDING_VIEWED.eventName
    )

    data class AmountEntered(val sourceTicker: String) : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_AMOUNT_ENTERED.eventName,
        params = mapOf(INPUT_CURRENCY to sourceTicker)
    )

    object SelectSourceOpened : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_SELECT_SOURCE_OPENED.eventName
    )

    object SelectDestinationOpened : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_SELECT_DESTINATION_OPENED.eventName
    )

    data class DestinationNotFound(val searchTerm: String) : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_DESTINATION_NOT_FOUND.eventName,
        params = mapOf(TEXT_SEARCHED to searchTerm)
    )

    data class DestinationSelected(val ticker: String) : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_OUTPUT_SELECTED.eventName,
        params = mapOf(OUTPUT_CURRENCY to ticker)
    )

    object ApproveTokenClicked : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_APPROVE_TOKEN_CLICKED.eventName
    )

    object ApproveTokenConfirmed : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_APPROVE_TOKEN_CONFIRMED.eventName
    )

    object SettingsOpened : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SETTINGS_OPENED.eventName
    )

    object SlippageChanged : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SLIPPAGE_CHANGED.eventName
    )

    data class PreviewViewed(
        val inputTicker: String,
        val inputAmount: String,
        val outputTicker: String,
        val outputAmount: String,
        val minOutputAmount: String,
        val slippage: String,
        val networkFee: String,
        val networkFeeTicker: String,
        val blockchainFee: String,
        val blockchainFeeTicker: String,
        val inputNetwork: String,
        val outputNetwork: String,
    ) : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_PREVIEW_VIEWED.eventName,
        params = mapOf(
            INPUT_CURRENCY to inputTicker,
            INPUT_AMOUNT to inputAmount,
            OUTPUT_CURRENCY to outputTicker,
            EXPECTED_OUTPUT_AMOUNT to outputAmount,
            MIN_OUTPUT_AMOUNT to minOutputAmount,
            SLIPPAGE_ALLOWED to slippage,
            NEWORK_FEE_AMOUNT to networkFee,
            NETWORK_FEE_CURRENCY to networkFeeTicker,
            BLOCKCHAIN_FEE_AMOUNT to blockchainFee,
            BLOCKCHAIN_FEE_CURRENCY to blockchainFeeTicker,
            INPUT_NETWORK to inputNetwork,
            OUTPUT_NETWORK to outputNetwork
        )
    )

    data class ConfirmSwapClicked(
        val inputTicker: String,
        val inputAmount: String,
        val outputTicker: String,
        val outputAmount: String,
        val minOutputAmount: String,
        val slippage: String,
        val networkFee: String,
        val networkFeeTicker: String,
        val blockchainFee: String,
        val blockchainFeeTicker: String,
        val inputNetwork: String,
        val outputNetwork: String,
    ) : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_CONFIRM_CLICKED.eventName,
        params = mapOf(
            INPUT_CURRENCY to inputTicker,
            INPUT_AMOUNT to inputAmount,
            OUTPUT_CURRENCY to outputTicker,
            EXPECTED_OUTPUT_AMOUNT to outputAmount,
            MIN_OUTPUT_AMOUNT to minOutputAmount,
            SLIPPAGE_ALLOWED to slippage,
            NEWORK_FEE_AMOUNT to networkFee,
            NETWORK_FEE_CURRENCY to networkFeeTicker,
            BLOCKCHAIN_FEE_AMOUNT to blockchainFee,
            BLOCKCHAIN_FEE_CURRENCY to blockchainFeeTicker,
            INPUT_NETWORK to inputNetwork,
            OUTPUT_NETWORK to outputNetwork
        )
    )

    object InProgressViewed : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_IN_PROGRESS_VIEWED.eventName
    )

    object ExecutedViewed : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_EXECUTED_VIEWED.eventName
    )

    object FailedViewed : DexAnalyticsEvents(
        event = AnalyticsNames.DEX_SWAP_FAILED_VIEWED.eventName
    )

    companion object {
        private const val TEXT_SEARCHED = "text_searched"
        private const val OUTPUT_CURRENCY = "output_currency"
        private const val INPUT_CURRENCY = "input_currency"
        private const val INPUT_AMOUNT = "input_amount"
        private const val EXPECTED_OUTPUT_AMOUNT = "expected_output_amount"
        private const val MIN_OUTPUT_AMOUNT = "min_output_amount"
        private const val SLIPPAGE_ALLOWED = "slippage_allowed"
        private const val NEWORK_FEE_AMOUNT = "nework_fee_amount"
        private const val NETWORK_FEE_CURRENCY = "network_fee_currency"
        private const val BLOCKCHAIN_FEE_AMOUNT = "blockchain_fee_amount"
        private const val BLOCKCHAIN_FEE_CURRENCY = "blockchain_fee_currency"
        private const val INPUT_NETWORK = "input_network"
        private const val OUTPUT_NETWORK = "output_network"
    }
}
