package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class AppRatingViewState(
    val dismiss: Boolean,
    val promptInAppReview: Boolean
) : ViewState
