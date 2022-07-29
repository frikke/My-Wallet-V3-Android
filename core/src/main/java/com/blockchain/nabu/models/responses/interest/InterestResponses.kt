package com.blockchain.nabu.models.responses.interest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterestAddressResponse(
    @SerialName("accountRef") val address: String
)

@Serializable
data class InterestWithdrawalBody(
    val withdrawalAddress: String,
    val amount: String,
    val currency: String
)
