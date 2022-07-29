package com.blockchain.core.interest.domain.model

import com.blockchain.nabu.models.responses.simplebuy.TransactionBeneficiaryResponse
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import java.util.Date

data class InterestActivity(
    val value: CryptoValue,
    val asset: AssetInfo,
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
    val beneficiary: TransactionBeneficiaryResponse? = null
)