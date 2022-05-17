package piuk.blockchain.android.rating.presentaion

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface AppRatingNavigationEvent : NavigationEvent {
    object RequestInAppReview : AppRatingNavigationEvent
    object Feedback : AppRatingNavigationEvent
    data class Completed(val withFeedback: Boolean) : AppRatingNavigationEvent
    object Dismiss : AppRatingNavigationEvent
}
