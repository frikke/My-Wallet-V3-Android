package com.blockchain.api.earn.active

import kotlinx.serialization.Serializable

@Serializable
data class EarnWithdrawalDto(
    val product: String,
    val currency: String,
    val userId: String,
    val maxRequested: Boolean,
    val amount: String? = null,
    val unbondingStartDate: String? = null,
    val unbondingExpiry: String? = null
)
