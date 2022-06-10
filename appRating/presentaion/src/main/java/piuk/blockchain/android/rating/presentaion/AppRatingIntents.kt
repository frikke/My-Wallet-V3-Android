package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface AppRatingIntents : Intent<AppRatingModelState> {
    data class StarsSubmitted(val stars: Int) : AppRatingIntents
    data class FeedbackSubmitted(val feedback: String) : AppRatingIntents
    data class InAppReviewRequested(val successful: Boolean) : AppRatingIntents
    object RatingCanceled : AppRatingIntents
}
