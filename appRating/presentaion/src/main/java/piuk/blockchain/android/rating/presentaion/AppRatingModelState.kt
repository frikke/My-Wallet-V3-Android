package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class AppRatingModelState(
    val dismiss: Boolean = false,
    val promptInAppReview: Boolean = false,
    val isLoading: Boolean = false,

    val stars: Int = 0,
    val walletId: String = "",
    val screenName: String = "",
    val feedback: String = ""
) : ModelState {
    companion object {
        private const val SEPARATOR = ", ------ "
        private const val SCREEN = "Screen: "
        private const val WALLET_ID = "Wallet id: "
    }

    fun feedbackFormatted(): String = StringBuilder().apply {
        if (feedback.isBlank().not()) {
            append(feedback)
            // to separate feedback from wallet id
            // apparently new lines don't register as such,
            // even when doing it on web, the result is in one line
            append(SEPARATOR)
        }

        append("$WALLET_ID$walletId")
        append(SEPARATOR)
        append("$SCREEN$screenName")
    }.toString()
}
