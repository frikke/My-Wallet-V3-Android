package com.blockchain.domain.paymentmethods.model

@kotlinx.serialization.Serializable
data class DepositTerms(
    val creditCurrency: String,
    val availableToTradeMinutesMin: Int,
    val availableToTradeMinutesMax: Int,
    val availableToTradeDisplayMode: DisplayMode,
    val availableToWithdrawMinutesMin: Int,
    val availableToWithdrawMinutesMax: Int,
    val availableToWithdrawDisplayMode: DisplayMode,
    val settlementType: SettlementType?,
    val settlementReason: SettlementReason?
) {
    enum class DisplayMode {
        DAY_RANGE,
        MAX_DAY,
        MINUTE_RANGE,
        MAX_MINUTE,
        IMMEDIATELY,
        NONE // when reason is null
    }
}
