package com.blockchain.earn.domain.models.interest

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import java.util.Date

data class InterestActivity(
    val value: CryptoValue,
    val fiatValue: Money?,
    val id: String,
    val insertedAt: Date,
    val state: InterestState,
    val type: TransactionSummary.TransactionType,
    val extraAttributes: InterestActivityAttributes?
)

enum class InterestState {
    PROCESSING,
    PENDING,
    MANUAL_REVIEW,
    CLEARED,
    REFUNDED,
    FAILED,
    REJECTED,
    COMPLETE,
    UNKNOWN
}

data class InterestActivityAttributes(
    val address: String? = null,
    val confirmations: Int? = 0,
    val hash: String? = null,
    val id: String? = null,
    val transactionHash: String? = null,
    val transferType: String? = null,
    val beneficiary: InterestTransactionBeneficiary? = null
)

data class InterestTransactionBeneficiary(
    val accountRef: String?,
    val user: String?
)
