package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class AppRatingModelState(
    val dismiss: Boolean = false,
    val promptInAppReview: Boolean = false,
    val isLoading: Boolean = false,

    val stars: Int = 0,
    val feedback: StringBuilder = StringBuilder(),
    /**
     * [AppRatingViewModel.inAppReviewCompleted] could return an error because showing in-app could've failed
     */
    var forceRetrigger: Boolean = false
) : ModelState
