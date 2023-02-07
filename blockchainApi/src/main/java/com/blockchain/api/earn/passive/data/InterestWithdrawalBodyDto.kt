package com.blockchain.api.earn.passive.data

import kotlinx.serialization.Serializable

@Serializable
data class InterestWithdrawalBodyDto(
    val withdrawalAddress: String,
    val amount: String,
    val currency: String
)
