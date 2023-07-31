package com.blockchain.core.price.historic

import kotlinx.serialization.Serializable

@Serializable
data class HistoricRate(
    val rate: Double,
    val fiatTicker: String,
    val assetTicker: String,
    val requestedTimestamp: Long
)
