package com.blockchain.domain.trade.model

import kotlinx.serialization.Serializable

@Serializable
sealed class QuickFillRoundingData {
    @Serializable
    data class SellSwapRoundingData(
        val multiplier: Float,
        val rounding: List<Int>
    ) : QuickFillRoundingData()

    @Serializable
    data class BuyRoundingData(
        val multiplier: Int,
        val rounding: Int
    ) : QuickFillRoundingData()
}
