package piuk.blockchain.android.ui.transactionflow.engine.domain.model

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

@Serializable
data class SellSwapRounding(
    val multiplier: Float,
    val rounding: List<Int>
)

@Serializable
data class BuyRounding(
    val multiplier: Float,
    val rounding: Int
)
