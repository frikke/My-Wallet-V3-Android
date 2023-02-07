package com.blockchain.earn.domain.models

data class EarnRewardsActivityAttributes(
    val address: String? = null,
    val confirmations: Int? = 0,
    val hash: String? = null,
    val id: String? = null,
    val transactionHash: String? = null,
    val transferType: String? = null,
    val beneficiary: EarnRewardsTransactionBeneficiary? = null
)
