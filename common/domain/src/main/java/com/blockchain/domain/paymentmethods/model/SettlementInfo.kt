package com.blockchain.domain.paymentmethods.model

data class SettlementInfo(
    val partner: BankPartner?,
    val state: BankState,
    val settlementType: SettlementType,
    val settlementReason: SettlementReason
)

enum class SettlementReason {
    INSUFFICIENT_BALANCE,
    STALE_BALANCE,
    REQUIRES_UPDATE,
    GENERIC,
    UNKNOWN,
    NONE // when reason is null
}

enum class SettlementType {
    INSTANT,
    REGULAR,
    UNAVAILABLE,
    UNKNOWN, // unhandled type
}
