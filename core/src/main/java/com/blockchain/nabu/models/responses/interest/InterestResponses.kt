package com.blockchain.nabu.models.responses.interest

import kotlinx.serialization.Serializable

@Serializable
data class InterestWithdrawalBody(
    val withdrawalAddress: String,
    val amount: String,
    val currency: String
)
