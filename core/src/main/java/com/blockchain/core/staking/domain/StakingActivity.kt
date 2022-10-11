package com.blockchain.core.staking.domain

import com.blockchain.nabu.models.responses.simplebuy.TransactionBeneficiaryResponse
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import java.util.Date

data class StakingActivity(
    val value: CryptoValue,
    val id: String,
    val insertedAt: Date,
    val state: StakingState,
    val type: TransactionSummary.TransactionType,
    val extraAttributes: StakingActivityAttributes?
)

enum class StakingState {
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

data class StakingActivityAttributes(
    val address: String? = null,
    val confirmations: Int? = 0,
    val hash: String? = null,
    val id: String? = null,
    val transactionHash: String? = null,
    val transferType: String? = null,
    val beneficiary: TransactionBeneficiaryResponse? = null
)
