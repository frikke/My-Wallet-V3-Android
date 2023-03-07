package com.blockchain.api.earn.active.data

import kotlinx.serialization.Serializable

@Serializable
data class ActiveRewardsWithdrawalDto(
    val product: String,
    val currency: String,
    val userId: String,
    val maxRequested: Boolean
)
