package com.blockchain.api.custodial.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TradingBalanceResponseDto(
    @SerialName("pending")
    val pending: String,
    @SerialName("available") // Badly named param, is actually the total including uncleared & locked
    val total: String,
    @SerialName("withdrawable") // Balance that is NOT uncleared and IS withdrawable
    val withdrawable: String,
    val mainBalanceToDisplay: String, // This is be available + processing payments(ie haven't settled yet)
)
